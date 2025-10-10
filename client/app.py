from flask import Flask, render_template, request, redirect, url_for, session, flash
from utils import mapear_rol, requiere_autenticacion, requiere_rol_presidente, requiere_rol_presidente_o_coordinador, requiere_rol_presidente_o_vocal
from cliente_usuario import ClienteUsuario
from cliente_evento import ClienteEvento
from cliente_donacion import ClienteDonacion
from proto import usuarioService_pb2 as usuario_pb2
from proto import usuarioService_pb2_grpc as usuario_pb2_grpc
from proto import authService_pb2 as auth_pb2
from proto import authService_pb2_grpc as auth_pb2_grpc
from datetime import datetime
import atexit

app = Flask(
    __name__,
    template_folder="views/templates",
    static_folder="views/static"
)
app.secret_key = "super_secret_key"
cliente_usuario = ClienteUsuario()
cliente_evento = ClienteEvento()
cliente_donacion = ClienteDonacion()
atexit.register(cliente_usuario.cerrar)
atexit.register(cliente_evento.cerrar)
atexit.register(cliente_evento.cerrar)

def obtener_usuarios():
    """Obtiene la lista de usuarios desde el servidor."""
    try:
        response = cliente_usuario.listar_usuarios(session['token'])
        if response is None:
            flash("Error al obtener la lista de usuarios", "error")
            return []
        return response.usuarios
    except Exception as e:
        print(f"Error al obtener usuarios: {e}")
        flash("Error al obtener la lista de usuarios", "error")
        return []
    
def obtener_eventos():
    """Obtiene la lista de eventos desde el servidor."""
    try:
        response = cliente_evento.list_eventos(session['token'])
        if response is None or response.status != "SUCCESS":
            flash(response.message if response else "Error al obtener la lista de eventos", "error")
            return []
        return response.eventos
    except Exception as e:
        print(f"Error al obtener eventos: {e}")
        flash("Error al obtener la lista de eventos", "error")
        return []
    

@app.template_filter('datetimeformat')
def datetimeformat(value):
    try:
        dt = datetime.fromisoformat(value.replace('Z', '+00:00'))
        return dt.strftime('%Y-%m-%dT%H:%M')
    except Exception:
        return value

@app.route('/', methods=['GET'])
@requiere_autenticacion(cliente_usuario)
def index():
    usuario = cliente_usuario.traer_usuario_por_email(session['email'], session['token'])
    if usuario is None:
        session.clear()
        flash("Error al obtener datos del usuario", "error")
        return redirect(url_for('login'))
    
    usuario_dict = {
        "id": usuario.id,
        "nombreUsuario": usuario.nombreUsuario,
        "nombre": usuario.nombre,
        "apellido": usuario.apellido,
        "telefono": usuario.telefono,
        "email": usuario.email,
        "rol": mapear_rol(usuario.rol),
        "activo": usuario.activo
    }
    
    es_presidente = usuario.rol == 0
    return render_template('perfil.html', usuario=usuario_dict, es_presidente=es_presidente)

@app.route('/login', methods=['GET', 'POST'])
def login():
    if 'token' in session:
        try:
            response = cliente_usuario.validar_token(session['token'])
            if response and response.status == "SUCCESS":
                flash("Ya estás logueado", "info")
                return redirect(url_for('index'))
        except Exception:
            session.clear()
    if request.method == 'POST':
        email = request.form['email']
        clave = request.form['clave']
        try:
            response = cliente_usuario.login(email, clave)
            print(f"Respuesta de login: {response}")
            if response is None:
                flash("Error en el servidor, intenta nuevamente", "error")
                return render_template('login.html')
            if response.status == "SUCCESS":
                session['token'] = response.token
                session['email'] = email
                flash("Inicio de sesión exitoso", "success")
                return redirect(url_for('index'))
            else:
                flash(response.message, "error")
                return render_template('login.html')
        except Exception as e:
            print(f"Error al iniciar sesión: {e}")
            flash("Error al iniciar sesión, intenta nuevamente", "error")
            return render_template('login.html')
    return render_template('login.html')

@app.route('/logout')
def logout():
    if 'token' in session:
        try:
            response = cliente_usuario.logout(session['token'])
            if response and response.status == "SUCCESS":
                flash("Cierre de sesión exitoso", "success")
            else:
                flash("Error al cerrar sesión: " + (response.message if response else "Desconocido"), "error")
        except Exception as e:
            print(f"Error al cerrar sesión: {e}")
            flash("Error al cerrar sesión", "error")
    session.clear()
    return redirect(url_for('login'))



@app.route('/usuarios', methods=['GET'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente(cliente_usuario)
def usuarios():
    usuarios = obtener_usuarios()
    return render_template('usuarios.html', usuarios=usuarios)



@app.route('/registrar', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente(cliente_usuario)
def registrar():
    
    if request.method == 'POST':
        nombre_usuario = request.form['nombreUsuario']
        nombre = request.form['nombre']
        apellido = request.form['apellido']
        telefono = request.form['telefono']
        email = request.form['email']
        rol = request.form['rol']
        
        try:
            response = cliente_usuario.registrar_usuario(nombre_usuario, nombre, apellido, telefono, email, rol, session['token'])
            if response.status == "SUCCESS":
                flash("Usuario registrado exitosamente", "success")
                return redirect(url_for('index'))
            else:
                flash(response.message, "error")
        except Exception as e:
            print(f"Error al registrar usuario: {e}")
            flash("Error al registrar usuario", "error")
    
    return render_template('registrar.html')


@app.route('/cuenta', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
def cuenta():
    usuario = cliente_usuario.traer_usuario_por_email(session['email'], session['token'])
    if usuario is None:
        session.clear()
        flash("Error al obtener datos del usuario", "error")
        return redirect(url_for('login'))
    
    if request.method == 'POST':
        if 'eliminar' in request.form:
            try:
                response = cliente_usuario.eliminar_usuario(usuario.id, session['token'])
                if response and response.success:
                    flash("Usuario dado de baja exitosamente", "success")
                    session.clear()
                    return redirect(url_for('login'))
                else:
                    flash(response.message if response else "Error al eliminar cuenta", "error")
            except Exception as e:
                print(f"Error al eliminar cuenta: {e}")
                flash("Error al eliminar cuenta", "error")
        else:
            nombre_usuario = request.form['nombreUsuario']
            nombre = request.form['nombre']
            apellido = request.form['apellido']
            telefono = request.form['telefono']
            email = request.form['email']
            rol = mapear_rol(usuario.rol)  
            try:
                response = cliente_usuario.modificar_usuario(usuario.id, nombre_usuario, nombre, apellido, telefono, email, rol, session['token'])
                if response.status == "SUCCESS":
                    session['email'] = email
                    flash(response.message, "success")
                    return redirect(url_for('index'))
                else:
                    flash(response.message, "error")
            except Exception as e:
                print(f"Error al modificar cuenta: {e}")
                flash("Error al modificar cuenta", "error")
    
    usuario_dict = {
        "id": usuario.id,
        "nombreUsuario": usuario.nombreUsuario,
        "nombre": usuario.nombre,
        "apellido": usuario.apellido,
        "telefono": usuario.telefono,
        "email": usuario.email,
        "rol": mapear_rol(usuario.rol),
        "activo": usuario.activo
    }
    return render_template('cuenta.html', usuario=usuario_dict)

@app.route('/eventos', methods=['GET'])
@requiere_autenticacion(cliente_usuario)
def eventos():
    eventos = obtener_eventos()
    for evento in eventos:
        print(f"Evento: idEvento={evento.idEvento}, nombreEvento={evento.nombreEvento}, descripcion={evento.descripcion}, fechaHora={evento.fechaHora}")
    usuario = cliente_usuario.traer_usuario_por_email(session['email'], session['token'])
    if usuario is None:
        session.clear()
        flash("Error al obtener datos del usuario", "error")
        return redirect(url_for('index'))
    puede_administrar_eventos = usuario.rol in [0, 2]
    return render_template('eventos.html', eventos=eventos, puede_administrar_eventos=puede_administrar_eventos)

@app.route('/eventos/registrar', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_coordinador(cliente_usuario)
def registrar_evento():
    if request.method == 'POST':
        nombre_evento = request.form['nombreEvento']
        descripcion = request.form['descripcion']
        fecha_hora = request.form['fechaHora']
        usuario_ids = request.form.getlist('usuarioIds')  # Lista de IDs
        usuario_ids = [int(id) for id in usuario_ids if id]  # Convertir a enteros
        
        try:
            response = cliente_evento.create_evento(
                nombre_evento=nombre_evento,
                descripcion=descripcion,
                fecha_hora=fecha_hora,
                usuario_ids=usuario_ids,
                token=session['token']
            )
            print(f"Respuesta de create_evento: status={response.status}, message={response.message}")
            if response.status == "SUCCESS":
                flash("Evento registrado exitosamente", "success")
                return redirect(url_for('eventos'))
            else:
                flash(response.message or "Error al registrar evento", "error")
                return render_template('registrar_evento.html', usuarios=obtener_usuarios())
        except Exception as e:
            print(f"Error al registrar evento: {e}")
            flash(f"Error al registrar evento: {str(e)}", "error")
            return render_template('registrar_evento.html', usuarios=obtener_usuarios())
    
    usuarios = obtener_usuarios()  # Para seleccionar usuarios en el formulario
    return render_template('registrar_evento.html', usuarios=usuarios)

@app.route('/eventos/<int:id_evento>', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_coordinador(cliente_usuario)
def evento(id_evento):
    print(f"Accediendo a evento con id_evento={id_evento}, token={session.get('token')}")
    usuario = cliente_usuario.traer_usuario_por_email(session['email'], session['token'])
    print(f"Usuario: {usuario.email if usuario else None}, Rol: {usuario.rol if usuario else None}")
    
    if request.method == 'POST':
        if 'eliminar' in request.form:
            try:
                response = cliente_evento.delete_evento(id_evento, session['token'])
                print(f"Respuesta de delete_evento: success={response.success}, message={response.message}")
                if response.success:
                    flash("Evento eliminado exitosamente", "success")
                    return redirect(url_for('eventos'))
                else:
                    flash(response.message, "error")
            except Exception as e:
                print(f"Error al eliminar evento: {e}")
                flash(f"Error al eliminar evento: {str(e)}", "error")
        else:
            nombre_evento = request.form['nombreEvento']
            descripcion = request.form['descripcion']
            fecha_hora = request.form['fechaHora']
            usuario_ids = request.form.get('usuarioIds', '').split(',')
            usuario_ids = [int(id.strip()) for id in usuario_ids if id.strip()]
            
            try:
                response = cliente_evento.update_evento(
                    id_evento=id_evento,
                    nombre_evento=nombre_evento,
                    descripcion=descripcion,
                    fecha_hora=fecha_hora,
                    usuario_ids=usuario_ids,
                    token=session['token']
                )
                print(f"Respuesta de update_evento: status={response.status}, message={response.message}")
                if response.status == "SUCCESS":
                    flash("Evento actualizado exitosamente", "success")
                    return redirect(url_for('eventos'))
                else:
                    flash(response.message, "error")
            except Exception as e:
                print(f"Error al actualizar evento: {e}")
                flash(f"Error al actualizar evento: {str(e)}", "error")
    
    response = cliente_evento.get_evento(id_evento, session['token'])
    print(f"Respuesta de get_evento: status={response.status if response else None}, message={response.message if response else None}")
    if response is None or response.status != "SUCCESS":
        flash(response.message if response else "Error al obtener el evento", "error")
        return redirect(url_for('eventos'))
        
    evento = response.evento
    usuarios = obtener_usuarios()
    puede_administrar_eventos = usuario.rol in [0, 2] if usuario else False
    
    return render_template('modificar_eliminar_evento.html', evento=evento, usuarios=usuarios, puede_administrar_eventos=puede_administrar_eventos)


@app.route('/donaciones', methods=['GET'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def donaciones():
    try:
            token = session.get('token')
            if not token:
                flash("Debes iniciar sesión primero", "error")
                return redirect(url_for('login'))
            
            controller = ClienteDonacion()
            response = controller.listar_donaciones(token)
            
            if response and hasattr(response, 'donaciones'):
                donaciones = response.donaciones
            else:
                donaciones = []
                flash("Error al obtener las donaciones", "error")
            
            return render_template('donaciones.html', donaciones=donaciones)
    except Exception as e:
            flash(f"Error: {str(e)}", "error")
            return render_template('donaciones.html', donaciones=[])

@app.route('/agregar_donacion', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def agregar_donacion():
    if request.method == 'POST':
            try:
                token = session.get('token')
                if not token:
                    flash("Debes iniciar sesión primero", "error")
                    return redirect(url_for('login'))
                
                categoria = request.form['categoria']
                descripcion = request.form['descripcion']
                cantidad = int(request.form['cantidad'])
                
                if cantidad <= 0:
                    flash("La cantidad debe ser mayor a 0", "error")
                    return render_template('agregar_donacion.html')
                
                controller = ClienteDonacion()
                response = controller.registrar_donacion(token, categoria, descripcion, cantidad)
                
                if response and hasattr(response, 'status') and response.status == "SUCCESS":
                    flash("Donación registrada exitosamente", "success")
                    return redirect(url_for('donaciones'))
                else:
                    message = getattr(response, 'message', 'Error al registrar donación') if response else "Error al registrar donación"
                    flash(message, "error")
                    return render_template('agregar_donacion.html')
                    
            except ValueError:
                flash("La cantidad debe ser un número válido", "error")
                return render_template('agregar_donacion.html')
            except Exception as e:
                flash(f"Error al registrar donación: {str(e)}", "error")
                return render_template('agregar_donacion.html')
        
    return render_template('agregar_donacion.html')

@app.route('/eliminar_donacion/<int:donacion_id>', methods=['POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def eliminar_donacion(donacion_id):
    try:
            token = session.get('token')
            if not token:
                flash("Debes iniciar sesión primero", "error")
                return redirect(url_for('login'))
            
            controller = ClienteDonacion()
            response = controller.eliminar_donacion(token, donacion_id)
            
            if response and hasattr(response, 'success') and response.success:
                flash("Donación eliminada exitosamente", "success")
            else:
                message = getattr(response, 'message', 'Error al eliminar donación') if response else "Error al eliminar donación"
                flash(message, "error")
                
    except Exception as e:
            flash(f"Error al eliminar donación: {str(e)}", "error")
        
    return redirect(url_for('donaciones'))

@app.route('/modificar_donacion/<int:donacion_id>', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente_o_vocal(cliente_usuario)
def modificar_donacion(donacion_id):
    if request.method == 'POST':
            try:
                token = session.get('token')
                if not token:
                    flash("Debes iniciar sesión primero", "error")
                    return redirect(url_for('login'))
                
                descripcion = request.form['descripcion']
                cantidad = int(request.form['cantidad'])
                
                # Validaciones
                if cantidad <= 0:
                    flash("La cantidad debe ser mayor a 0", "error")
                    return redirect(url_for('modificar_donacion', donacion_id=donacion_id))
                
                controller = ClienteDonacion()
                response = controller.modificar_donacion(token, donacion_id, descripcion, cantidad)
                
                if response and hasattr(response, 'status') and response.status == "SUCCESS":
                    flash("Donación modificada exitosamente", "success")
                    return redirect(url_for('donaciones'))
                else:
                    message = getattr(response, 'message', 'Error al modificar donación') if response else "Error al modificar donación"
                    flash(message, "error")
                    return redirect(url_for('modificar_donacion', donacion_id=donacion_id))
                    
            except ValueError:
                flash("La cantidad debe ser un número válido", "error")
                return redirect(url_for('modificar_donacion', donacion_id=donacion_id))
            except Exception as e:
                flash(f"Error al modificar donación: {str(e)}", "error")
                return redirect(url_for('modificar_donacion', donacion_id=donacion_id))
    try:
            token = session.get('token')
            if not token:
                flash("Debes iniciar sesión primero", "error")
                return redirect(url_for('login'))
            
            controller = ClienteDonacion()
            response = controller.traer_donacion_por_id(token, donacion_id)
            
            if response and hasattr(response, 'status') and response.status == "SUCCESS":
                donacion = response
                return render_template('modificar_donacion.html', donacion=donacion)
            else:
                flash("Error al cargar la donación", "error")
                return redirect(url_for('donaciones'))
                
    except Exception as e:
            flash(f"Error: {str(e)}", "error")
            return redirect(url_for('donaciones'))
    

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5001)

