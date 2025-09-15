from flask import Flask, render_template, request, redirect, url_for, session, flash
from cliente_usuario import ClienteUsuario
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
cliente = ClienteUsuario()
atexit.register(cliente.cerrar)

def obtener_usuarios():
    """Obtiene la lista de usuarios desde el servidor."""
    try:
        response = cliente.listar_usuarios(session['token'])
        if response is None:
            flash("Error al obtener la lista de usuarios", "error")
            return []
        return response.usuarios
    except Exception as e:
        print(f"Error al obtener usuarios: {e}")
        flash("Error al obtener la lista de usuarios", "error")
        return []

@app.route('/', methods=['GET'])
def index():
    if 'token' not in session:
        session.clear()
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('login'))
    
    response = cliente.validar_token(session['token'])
    if response is None or response.status != "SUCCESS":
        session.clear()
        flash("Sesión inválida, inicia sesión nuevamente", "error")
        return redirect(url_for('login'))
    
    usuario = cliente.traer_usuario_por_email(session['email'])
    if usuario is None:
        session.clear()
        flash("Error al obtener datos del usuario", "error")
        return redirect(url_for('login'))
    
    # Mapear el valor del enum rol al nombre
    rol_map = {
        0: "PRESIDENTE",
        1: "VOCAL",
        2: "COORDINADOR",
        3: "VOLUNTARIO"
    }
    usuario_dict = {
        "id": usuario.id,
        "nombreUsuario": usuario.nombreUsuario,
        "nombre": usuario.nombre,
        "apellido": usuario.apellido,
        "telefono": usuario.telefono,
        "email": usuario.email,
        "rol": rol_map.get(usuario.rol, "Desconocido"),
        "activo": usuario.activo
    }
    
    # Bandera para determinar si el usuario es PRESIDENTE
    es_presidente = usuario.rol == 0
    
    return render_template('perfil.html', usuario=usuario_dict, es_presidente=es_presidente)

@app.route('/login', methods=['GET', 'POST'])
def login():
    if 'token' in session:
        try:
            response = cliente.validar_token(session['token'])
            if response and response.status == "SUCCESS":
                flash("Ya estás logueado", "info")
                return redirect(url_for('index'))
        except Exception:
            session.clear()
    if request.method == 'POST':
        email = request.form['email']
        clave = request.form['clave']
        try:
            response = cliente.login(email, clave)
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
            response = cliente.logout()  
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
def usuarios():
    if 'token' not in session:
        session.clear()
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('login'))
    
    response = cliente.validar_token(session['token'])
    if response is None or response.status != "SUCCESS":
        session.clear()
        flash("Sesión inválida, inicia sesión nuevamente", "error")
        return redirect(url_for('login'))
    
    usuario = cliente.traer_usuario_por_email(session['email'])
    if usuario is None:
        session.clear()
        flash("Error al obtener datos del usuario", "error")
        return redirect(url_for('login'))
    
    if usuario.rol != 0:  # Solo PRESIDENTE puede acceder
        flash("No tienes permisos para gestionar usuarios", "error")
        return redirect(url_for('index'))
    
    usuarios = obtener_usuarios()
    return render_template('usuarios.html', usuarios=usuarios)

@app.route('/registrar', methods=['GET', 'POST'])
def registrar():
    if 'token' not in session:
        session.clear()
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('login'))
    
    response = cliente.validar_token(session['token'])
    if response is None or response.status != "SUCCESS":
        session.clear()
        flash("Sesión inválida, inicia sesión nuevamente", "error")
        return redirect(url_for('login'))
    
    usuario = cliente.traer_usuario_por_email(session['email'])
    if usuario is None or usuario.rol != 0:  # Solo PRESIDENTE puede registrar
        flash("No tienes permisos para registrar usuarios", "error")
        return redirect(url_for('index'))
    
    if request.method == 'POST':
        nombre_usuario = request.form['nombreUsuario']
        nombre = request.form['nombre']
        apellido = request.form['apellido']
        telefono = request.form['telefono']
        email = request.form['email']
        clave = request.form['clave']
        rol = request.form['rol']
        
        try:
            response = cliente.registrar_usuario(nombre_usuario, nombre, apellido, telefono, email, clave, rol, session['token'])
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
def cuenta():
    if 'token' not in session:
        session.clear()
        flash("Debes iniciar sesión primero", "error")
        return redirect(url_for('login'))
    
    response = cliente.validar_token(session['token'])
    if response is None or response.status != "SUCCESS":
        session.clear()
        flash("Sesión inválida, inicia sesión nuevamente", "error")
        return redirect(url_for('login'))
    
    usuario = cliente.traer_usuario_por_email(session['email'])
    if usuario is None:
        session.clear()
        flash("Error al obtener datos del usuario", "error")
        return redirect(url_for('login'))
    
    if request.method == 'POST':
        if 'eliminar' in request.form:
            try:
                response = cliente.eliminar_usuario(usuario.id, session['token'])
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
            clave = request.form.get('clave', '')
            rol = usuario.rol  # Mantener el rol actual
            
            try:
                response = cliente.modificar_usuario(usuario.id, nombre_usuario, nombre, apellido, telefono, email, rol, session['token'], clave)
                if response.status == "SUCCESS":
                    session['email'] = email
                    flash("Cuenta modificada exitosamente", "success")
                    return redirect(url_for('index'))
                else:
                    flash(response.message, "error")
            except Exception as e:
                print(f"Error al modificar cuenta: {e}")
                flash("Error al modificar cuenta", "error")
    
    # Mapear el valor del enum rol al nombre
    rol_map = {
        0: "PRESIDENTE",
        1: "VOCAL",
        2: "COORDINADOR",
        3: "VOLUNTARIO"
    }
    usuario_dict = {
        "id": usuario.id,
        "nombreUsuario": usuario.nombreUsuario,
        "nombre": usuario.nombre,
        "apellido": usuario.apellido,
        "telefono": usuario.telefono,
        "email": usuario.email,
        "rol": rol_map.get(usuario.rol, "Desconocido"),
        "activo": usuario.activo
    }
    
    return render_template('cuenta.html', usuario=usuario_dict)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5001)