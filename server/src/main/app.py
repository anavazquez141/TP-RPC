from proto import usuarioService_pb2 as usuario_pb2
from flask import Flask, render_template, request, redirect, url_for
from client.cliente_usuario import ClienteUsuario

# Indica las carpetas de templates y static
app = Flask(
    __name__,
    template_folder="views/templates",
    static_folder="views/static"
)
cliente = ClienteUsuario()

# ---------------- RUTAS ----------------

# Página principal: muestra tabla y formulario
@app.route('/', methods=['GET'])
def index():
    usuarios = cliente.listar_usuarios().usuarios
    return render_template('usuarios.html', usuarios=usuarios)

# Registrar usuario
@app.route('/registrar', methods=['POST'])
def registrar():
    data = request.form
    respuesta = cliente.registrar_usuario(
        nombreUsuario=data['nombreUsuario'],
        nombre=data['nombre'],
        apellido=data['apellido'],
        telefono=data['telefono'],
        email=data['email'],
        rol=getattr(usuario_pb2.Rol, data['rol']),  # convierte string a enum
        activo=True,
        clave=data['clave']
    )
    usuarios = cliente.listar_usuarios().usuarios
    return render_template('usuarios.html', usuarios=usuarios, mensaje=respuesta.message)

# Modificar usuario
@app.route('/modificar/<int:user_id>', methods=['GET', 'POST'])
def modificar(user_id):
    if request.method == 'POST':
        data = request.form
        cliente.modificar_usuario(
            user_id,
            data['nombreUsuario'], data['nombre'], data['apellido'],
            data['telefono'], data['email'],
            getattr(usuario_pb2.Rol, data['rol']),
            True,
            data['clave']
        )
        return redirect(url_for('index'))
    else:
        usuario = cliente.traer_usuario_por_id(user_id)
        return render_template('modificar_usuario.html', usuario=usuario)

# Eliminar usuario
@app.route('/eliminar/<int:user_id>', methods=['GET'])
def eliminar(user_id):
    respuesta = cliente.eliminar_usuario(user_id)
    usuarios = cliente.listar_usuarios().usuarios
    return render_template('usuarios.html', usuarios=usuarios, mensaje=respuesta.message)

# ---------------- BLOQUE PRINCIPAL ----------------
if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5001)

    