import grpc
from proto import usuarioService_pb2 as usuario_pb2
from proto import usuarioService_pb2_grpc as usuario_pb2_grpc
from proto import authService_pb2 as auth_pb2
from proto import authService_pb2_grpc as auth_pb2_grpc

class ClienteUsuario:
    def __init__(self, host='localhost', port=9090):
        self.host = host
        self.port = port
        self.channel = None
        self.usuario_stub = None
        self.auth_stub = None
        self.token = None

    def connect(self):
        if self.channel is None or self.is_channel_closed():
            self.channel = grpc.insecure_channel(f"{self.host}:{self.port}")
            self.usuario_stub = usuario_pb2_grpc.UsuarioServiceStub(self.channel)
            self.auth_stub = auth_pb2_grpc.AuthServiceStub(self.channel)

    def is_channel_closed(self):
        try:
            self.channel.subscribe(lambda connectivity: None)
            return False
        except Exception:
            return True

    def login(self, email, password):
        """Realiza el login y almacena el token si es exitoso."""
        self.connect()
        if self.token is not None:
            validation_response = self.validar_token(self.token)
            if validation_response and validation_response.status == "SUCCESS":
                print(f"Token existente válido: {self.token}")
                return auth_pb2.LoginResponse(status="SUCCESS", message="Ya has iniciado sesión", token=self.token)
            else:
                print(f"Token existente inválido: {self.token}, limpiando token")
                self.token = None  

        request = auth_pb2.LoginRequest(email=email, clave=password)
        try:
            response = self.auth_stub.login(request)
            if response.status == "SUCCESS":
                self.token = response.token
                return response
            else:
                return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return None
        except Exception as e:
            print(f"Error inesperado: {str(e)}")
            return None

    def logout(self):
        """Cierra la sesión y elimina el token."""
        self.connect()
        if not self.token:
            return auth_pb2.LogoutResponse(status="FAILURE", message="No hay token para cerrar sesión")
        request = auth_pb2.LogoutRequest(token=self.token)
        try:
            response = self.auth_stub.logout(request)
            if response.status == "SUCCESS":
                self.token = None
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return None

    def validar_token(self, token):
        """Valida un token con el servidor."""
        self.connect()
        request = auth_pb2.TokenValidationRequest(token=token)
        try:
            response = self.auth_stub.validarToken(request)
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return None

    def traer_usuario_por_email(self, email):
        self.connect()
        request = usuario_pb2.UsuarioEmailRequest(email=email)
        try:
            response = self.usuario_stub.traerUsuarioPorEmail(request)
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return None

    def traer_usuario_por_id(self, user_id, token):
        self.connect()
        request = usuario_pb2.UsuarioIdRequest(id=user_id, token=token)
        try:
            response = self.usuario_stub.traerUsuarioPorId(request)
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return None

    def listar_usuarios(self, token):
        self.connect()
        request = usuario_pb2.ListarUsuariosRequest(token=token)
        try:
            response = self.usuario_stub.listarUsuarios(request)
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return None

    def registrar_usuario(self, nombre_usuario, nombre, apellido, telefono, email, rol, token):
        self.connect()
        print(f"Enviando token para registrar_usuario: {token}")
        rol_map = {
            "PRESIDENTE": 0,
            "VOCAL": 1,
            "COORDINADOR": 2,
            "VOLUNTARIO": 3
        }
        request = usuario_pb2.UsuarioRequest(
            nombreUsuario=nombre_usuario,
            nombre=nombre,
            apellido=apellido,
            telefono=telefono,
            email=email,
            rol=rol_map.get(rol, 3),  # Por defecto VOLUNTARIO
            token=token
        )
        try:
            response = self.usuario_stub.registrarUsuario(request)
            print(f"Respuesta de registrarUsuario: {response}")
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return None

    def modificar_usuario(self, user_id, nombre_usuario, nombre, apellido, telefono, email, rol, token):
        """Modifica un usuario existente."""
        self.connect()
        rol_map = {
            0: 0,
            1: 1,
            2: 2,
            3: 3,
            "PRESIDENTE": 0,
            "VOCAL": 1,
            "COORDINADOR": 2,
            "VOLUNTARIO": 3
        }
        request = usuario_pb2.UsuarioRequest(
            id=user_id,
            nombreUsuario=nombre_usuario,
            nombre=nombre,
            apellido=apellido,
            telefono=telefono,
            email=email,
            rol=rol_map.get(rol, 3),  # Por defecto VOLUNTARIO
            token=token
        )
        try:
            response = self.usuario_stub.modificarUsuario(request)
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return None

    def eliminar_usuario(self, user_id, token):
        self.connect()
        print(f"Enviando solicitud para eliminar_usuario: id={user_id}, token={token}")
        request = usuario_pb2.UsuarioIdRequest(id=user_id, token=token)
        try:
            response = self.usuario_stub.eliminarUsuario(request)
            print(f"Respuesta de eliminarUsuario: success={response.success}, message={response.message}")
            if response.success:
                self.token = None  
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return None
        
    def cerrar(self):
        """Cierra el canal gRPC."""
        if self.channel is not None:
            self.channel.close()
            self.channel = None