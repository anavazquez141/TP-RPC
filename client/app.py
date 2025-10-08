from flask import Flask, render_template, request, redirect, url_for, session, flash
from utils import mapear_rol, requiere_autenticacion, requiere_rol_presidente
from cliente_usuario import ClienteUsuario
from cliente_evento import ClienteEvento
from proto import usuarioService_pb2 as usuario_pb2
from proto import usuarioService_pb2_grpc as usuario_pb2_grpc
from proto import authService_pb2 as auth_pb2
from proto import authService_pb2_grpc as auth_pb2_grpc
import atexit

app = Flask(
    __name__,
    template_folder="views/templates",
    static_folder="views/static"
)
app.secret_key = "super_secret_key"
cliente_usuario = ClienteUsuario()
cliente_evento = ClienteEvento()
atexit.register(cliente_usuario.cerrar)
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
    usuario = cliente_usuario.traer_usuario_por_email(session['email'], session['token'])
    if usuario is None:
        session.clear()
        flash("Error al obtener datos del usuario", "error")
        return redirect(url_for('login'))
    es_presidente = usuario.rol == 0
    return render_template('eventos.html', eventos=eventos, es_presidente=es_presidente)

@app.route('/eventos/registrar', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_presidente(cliente_usuario)
@requiere_rol_coordinador(cliente_usuario)
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
            if response.status == "SUCCESS":
                flash("Evento registrado exitosamente", "success")
                return redirect(url_for('eventos'))
            else:
                flash(response.message, "error")
        except Exception as e:
            print(f"Error al registrar evento: {e}")
            flash("Error al registrar evento", "error")
    
    usuarios = obtener_usuarios()  # Para seleccionar usuarios en el formulario
    return render_template('registrar_evento.html', usuarios=usuarios)

@app.route('/eventos/<int:id_evento>', methods=['GET', 'POST'])
@requiere_autenticacion(cliente_usuario)
@requiere_rol_coordinador(cliente_usuario)
def evento(id_evento):
    if request.method == 'POST':
        if 'eliminar' in request.form:
            try:
                response = cliente_evento.delete_evento(id_evento, session['token'])
                if response.success:
                    flash("Evento eliminado exitosamente", "success")
                    return redirect(url_for('eventos'))
                else:
                    flash(response.message, "error")
            except Exception as e:
                print(f"Error al eliminar evento: {e}")
                flash("Error al eliminar evento", "error")
        else:
            nombre_evento = request.form['nombreEvento']
            descripcion = request.form['descripcion']
            fecha_hora = request.form['fechaHora']
            usuario_ids = request.form.getlist('usuarioIds')
            usuario_ids = [int(id) for id in usuario_ids if id]
            
            try:
                response = cliente_evento.update_evento(
                    id_evento=id_evento,
                    nombre_evento=nombre_evento,
                    descripcion=descripcion,
                    fecha_hora=fecha_hora,
                    usuario_ids=usuario_ids,
                    token=session['token']
                )
                if response.status == "SUCCESS":
                    flash("Evento actualizado exitosamente", "success")
                    return redirect(url_for('eventos'))
                else:
                    flash(response.message, "error")
            except Exception as e:
                print(f"Error al actualizar evento: {e}")
                flash("Error al actualizar evento", "error")
    
    response = cliente_evento.get_evento(id_evento, session['token'])
    if response is None or response.status != "SUCCESS":
        flash(response.message if response else "Error al obtener el evento", "error")
        return redirect(url_for('eventos'))
    
    evento = response.evento
    usuarios = obtener_usuarios()
    usuario = cliente_usuario.traer_usuario_por_email(session['email'], session['token'])
    es_presidente = usuario.rol == 0 if usuario else False
    return render_template('evento.html', evento=evento, usuarios=usuarios, es_presidente=es_presidente)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5001)
    