import grpc
#import usuario_pb2
#import usuario_pb2_grpc
from proto import usuarioService_pb2 as usuario_pb2
from proto import usuarioService_pb2_grpc as usuario_pb2_grpc

class ClienteUsuario:
    def __init__(self, host='localhost', port=9090):
        self.channel = grpc.insecure_channel(f"{host}:{port}")
        self.stub = usuario_pb2_grpc.UsuarioServiceStub(self.channel)

    def registrar_usuario(self, nombreUsuario, nombre, apellido, telefono, email, rol, activo, clave):
        request = usuario_pb2.UsuarioRequest(
            nombreUsuario=nombreUsuario,
            nombre=nombre,
            apellido=apellido,
            telefono=telefono,
            email=email,
            rol=rol,
            activo=activo,
            clave=clave
        )
        return self.stub.registrarUsuario(request)

    def traer_usuario_por_id(self, user_id):
        request = usuario_pb2.UsuarioIdRequest(id=user_id)
        return self.stub.traerUsuarioPorId(request)

    def modificar_usuario(self, user_id, nombreUsuario, nombre, apellido, telefono, email, rol, activo, clave):
        request = usuario_pb2.UsuarioRequest(
            id=user_id,
            nombreUsuario=nombreUsuario,
            nombre=nombre,
            apellido=apellido,
            telefono=telefono,
            email=email,
            rol=rol,
            activo=activo,
            clave=clave
        )
        return self.stub.modificarUsuario(request)

    def eliminar_usuario(self, user_id):
        request = usuario_pb2.UsuarioIdRequest(id=user_id)
        return self.stub.eliminarUsuario(request)

    def listar_usuarios(self):
        request = usuario_pb2.ListarUsuariosRequest()
        return self.stub.listarUsuarios(request)

    def cerrar(self):
        self.channel.close()


# ------------------ EJEMPLO DE USO ------------------
if __name__ == "__main__":
    cliente = ClienteUsuario()

    # Registrar usuario
    resp = cliente.registrar_usuario(
        nombreUsuario="ana123",
        nombre="Ana",
        apellido="Belen",
        telefono="12345678",
        email="ana@example.com",
        rol=usuario_pb2.Rol.VOLUNTARIO,  # enum de proto
        activo=True,
        clave="secreto"
    )
    print("Registrar:", resp.id, resp.nombreUsuario, resp.nombre, resp.apellido, resp.email, resp.message)

    # Traer usuario por ID
    usuario = cliente.traer_usuario_por_id(resp.id)
    print("Traer por ID:", usuario.id, usuario.nombreUsuario, usuario.nombre, usuario.email, usuario.rol, usuario.activo)

    # Modificar usuario
    resp_mod = cliente.modificar_usuario(
        resp.id,
        nombreUsuario="ana456",
        nombre="Ana",
        apellido="Belen",
        telefono="87654321",
        email="ana2@example.com",
        rol=usuario_pb2.Rol.VOLUNTARIO,
        activo=True,
        clave="nuevoSecreto"
    )
    print("Modificar:", resp_mod.nombreUsuario, resp_mod.email, resp_mod.message)

    # Listar usuarios
    usuarios = cliente.listar_usuarios()
    for u in usuarios.usuarios:
        print("Usuario listado:", u.id, u.nombreUsuario, u.email, u.rol, u.activo)

    # Eliminar usuario
    resp_del = cliente.eliminar_usuario(resp.id)
    print("Eliminar:", resp_del.success, resp_del.message)

    cliente.cerrar()