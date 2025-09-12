from flask import Flask, render_template, request, redirect, url_for
from client.cliente_usuario import ClienteUsuario, usuario_pb2

app = Flask(__name__)
cliente = ClienteUsuario()  # Inicializa el cliente gRPC

# Helper para obtener la lista de usuarios
def obtener_usuarios():
    return cliente.listar_usuarios().usuarios

# Página principal: lista y formulario de registro
@app.route('/', methods=['GET'])
def index():
    usuarios = obtener_usuarios()
    return render_template('usuarios.html', usuarios=usuarios)

# Registrar usuario
@app.route('/registrar', methods=['POST'])
def registrar():
    nombreUsuario = request.form['nombreUsuario']
    nombre = request.form['nombre']
    apellido = request.form['apellido']
    telefono = request.form['telefono']
    email = request.form['email']
    clave = request.form['clave']
    rol_str = request.form['rol']

    # Mapear string a enum
    rol_enum = getattr(usuario_pb2.Rol, rol_str)

    respuesta = cliente.registrar_usuario(
        nombreUsuario, nombre, apellido, telefono, email, rol_enum, True, clave
    )

    usuarios = obtener_usuarios()
    return render_template('usuarios.html', usuarios=usuarios, mensaje=respuesta.message)

# Eliminar usuario
@app.route('/eliminar/<int:user_id>')
def eliminar(user_id):
    respuesta = cliente.eliminar_usuario(user_id)
    usuarios = obtener_usuarios()
    return render_template('usuarios.html', usuarios=usuarios, mensaje=respuesta.message)

# Modificar usuario
@app.route('/modificar/<int:user_id>', methods=['GET', 'POST'])
def modificar(user_id):
    if request.method == 'POST':
        nombreUsuario = request.form['nombreUsuario']
        nombre = request.form['nombre']
        apellido = request.form['apellido']
        telefono = request.form['telefono']
        email = request.form['email']
        clave = request.form['clave']
        rol_str = request.form['rol']

        rol_enum = getattr(usuario_pb2.Rol, rol_str)

        respuesta = cliente.modificar_usuario(
            user_id, nombreUsuario, nombre, apellido, telefono, email, rol_enum, True, clave
        )
        usuarios = obtener_usuarios()
        return render_template('usuarios.html', usuarios=usuarios, mensaje=respuesta.message)
    else:
        usuario = cliente.traer_usuario_por_id(user_id)
        return render_template('modificar_usuario.html', usuario=usuario)

# Cerrar cliente al terminar
@app.teardown_appcontext
def cerrar(exception):
    cliente.cerrar()

if __name__ == '__main__':
    app.run(debug=True)