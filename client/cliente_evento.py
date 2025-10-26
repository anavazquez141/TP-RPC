import grpc
from proto import eventoSolidarioService_pb2 as evento_pb2
from proto import eventoSolidarioService_pb2_grpc as evento_pb2_grpc
from proto import authService_pb2 as auth_pb2
from proto import authService_pb2_grpc as auth_pb2_grpc

class ClienteEvento:
    def __init__(self, host='localhost', port=9090):
        self.host = host
        self.port = port
        self.channel = None
        self.evento_stub = None
        self.auth_stub = None
        self.token = None

    def connect(self):
        """Establece la conexión gRPC con el servidor."""
        if self.channel is None or self.is_channel_closed():
            self.channel = grpc.insecure_channel(f"{self.host}:{self.port}")
            self.evento_stub = evento_pb2_grpc.EventosServiceStub(self.channel)
            self.auth_stub = auth_pb2_grpc.AuthServiceStub(self.channel)

    def is_channel_closed(self):
        """Verifica si el canal gRPC está cerrado."""
        try:
            self.channel.subscribe(lambda connectivity: None)
            return False
        except Exception:
            return True

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

    def create_evento(self, nombre_evento, descripcion, fecha_hora, usuario_ids=None, token=None):
        """Crea un evento enviando un CreateEventoRequest."""
        self.connect()
        token = token or self.token
        if not token:
            return evento_pb2.CreateEventoResponse(status="FAILURE", message="No hay token para crear evento")

        request = evento_pb2.CreateEventoRequest(
            token=token,
            nombreEvento=nombre_evento,
            descripcion=descripcion,
            fechaHora=fecha_hora,
            usuarioIds=usuario_ids or []
        )
        try:
            response = self.evento_stub.CreateEvento(request)
            print(f"Respuesta de createEvento: {response}")
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return evento_pb2.CreateEventoResponse(status="FAILURE", message=f"Error en el servidor: {e.details()}")

    def get_evento(self, id_evento, token=None):
        """Obtiene un evento por su ID enviando un GetEventoRequest."""
        self.connect()
        token = token or self.token
        if not token:
            return evento_pb2.GetEventoResponse(status="FAILURE", message="No hay token para obtener evento")

        request = evento_pb2.GetEventoRequest(
            token=token,
            idEvento=id_evento
        )
        try:
            response = self.evento_stub.GetEvento(request)
            print(f"Respuesta de getEvento: {response}")
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return evento_pb2.GetEventoResponse(status="FAILURE", message=f"Error en el servidor: {e.details()}")

    def update_evento(self, id_evento, nombre_evento=None, descripcion=None, fecha_hora=None, usuario_ids=None, token=None):
        """Actualiza un evento enviando un UpdateEventoRequest."""
        self.connect()
        token = token or self.token
        if not token:
            return evento_pb2.UpdateEventoResponse(status="FAILURE", message="No hay token para actualizar evento")

        request = evento_pb2.UpdateEventoRequest(
            token=token,
            idEvento=id_evento,
            nombreEvento=nombre_evento or "",
            descripcion=descripcion or "",
            fechaHora=fecha_hora or "",
            usuarioIds=usuario_ids or []
        )
        try:
            response = self.evento_stub.UpdateEvento(request)
            print(f"Respuesta de updateEvento: {response}")
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return evento_pb2.UpdateEventoResponse(status="FAILURE", message=f"Error en el servidor: {e.details()}")

    def delete_evento(self, id_evento, token=None):
        """Elimina un evento enviando un DeleteEventoRequest."""
        self.connect()
        token = token or self.token
        if not token:
            return evento_pb2.DeleteEventoResponse(success=False, message="No hay token para eliminar evento")

        request = evento_pb2.DeleteEventoRequest(
            token=token,
            idEvento=id_evento
        )
        try:
            response = self.evento_stub.DeleteEvento(request)
            print(f"Respuesta de deleteEvento: success={response.success}, message={response.message}")
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return evento_pb2.DeleteEventoResponse(success=False, message=f"Error en el servidor: {e.details()}")

    def list_eventos(self, token=None):
        """Lista todos los eventos enviando un ListEventosRequest."""
        self.connect()
        token = token or self.token
        if not token:
            return evento_pb2.ListEventosResponse(status="FAILURE", message="No hay token para listar eventos")

        request = evento_pb2.ListEventosRequest(
            token=token
        )
        try:
            response = self.evento_stub.ListEventos(request)
            print(f"Respuesta de listEventos: {response}")
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return evento_pb2.ListEventosResponse(status="FAILURE", message=f"Error en el servidor: {e.details()}")
                
    def asignarse_evento(self, id_evento, user_id, token=None):
        self.connect()
        token = token or self.token
        if not token:
            return evento_pb2.UpdateEventoResponse(status="FAILURE", message="No hay token para asignarse al evento")

        # Depuración: Imprimir el user_id recibido
        print(f"Construyendo UpdateEventoRequest con id_evento={id_evento}, user_id={user_id}, token={token}")

        request = evento_pb2.UpdateEventoRequest(
            token=token,
            idEvento=id_evento,
            usuarioIds=[user_id]
        )

        # Depuración: Imprimir el contenido del request
        print(f"UpdateEventoRequest: token={request.token}, idEvento={request.idEvento}, usuarioIds={request.usuarioIds}")

        try:
            response = self.evento_stub.AsignarseEvento(request)
            print(f"Respuesta de asignarse_evento: {response}")
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return evento_pb2.UpdateEventoResponse(status="FAILURE", message=f"Error en el servidor: {e.details()}") 


    def publicar_evento(self, token, id_evento, nombre_evento, descripcion, fecha_hora, id_organizacion):
        self.connect()

        
        if not all([nombre_evento, descripcion, fecha_hora, id_organizacion]):
            print("Error: faltan campos obligatorios para publicar el evento")
            return None

        request = evento_pb2.PublicarEventoRequest(
            token=token or "",
            idEvento=str(id_evento) if id_evento is not None else "",
            nombreEvento=nombre_evento,
            descripcion=descripcion,
            fechaHora=fecha_hora,
            idOrganizacion=str(id_organizacion)
        )

        try:
            response = self.evento_stub.PublicarEventoExterno(request, timeout=10)
            print(f"Respuesta publicar_evento: status={response.status}, message={response.message}")
            return response
        except grpc.RpcError as e:
            print(f"Error gRPC al publicar evento: {e.code().name} - {e.details()}")
            return None
        except Exception as e:
            print(f"Error inesperado al publicar evento: {str(e)}")
            return None


    def listar_eventos_externos(self, token):
        self.connect()

        request = evento_pb2.ListarEventosExternosRequest(token=token or "")

        try:
            response = self.evento_stub.ListarEventosExternos(request, timeout=10)
            print(f"Respuesta listar_eventos_externos: status={response.status}, message={response.message}")
            for evento in response.eventos:
                print(f"- {evento.nombreEvento} ({evento.idEvento}) de {evento.idOrganizacion}")
            return response
        except grpc.RpcError as e:
            print(f"Error gRPC al listar eventos externos: {e.code().name} - {e.details()}")
            return None
        except Exception as e:
            print(f"Error inesperado al listar eventos externos: {str(e)}")
            return None



    def cerrar(self):
        """Cierra el canal gRPC."""
        if self.channel is not None:
            self.channel.close()
            self.channel = None
            self.evento_stub = None
            self.auth_stub = None