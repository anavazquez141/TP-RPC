import grpc
from proto import donacionService_pb2 as donacion_pb2
from proto import donacionService_pb2_grpc as donacion_pb2_grpc

class ClienteDonacion:
    def __init__(self, host='localhost', port=9090):
        self.host = host
        self.port = port
        self.channel = None
        self.stub = None

    # Se conecta con el gRPC servidor
    def connect(self):
        if self.channel is None or self.is_channel_closed():
            self.channel = grpc.insecure_channel(f"{self.host}:{self.port}")
            self.stub = donacion_pb2_grpc(self.channel)

    #Verifica si el canal está cerrado
    def is_channel_closed(self):
        try:
            self.channel.subscribe(lambda connectivity: None, try_to_connect=True)
            return False
        except Exception:
            return True

    #Registra donación
    def registrar_donacion(self, token, categoria, descripcion, cantidad):
        self.connect()
        request = RegistrarDonacionRequest(
            token=token,
            categoria=categoria,
            descripcion=descripcion,
            cantidad=cantidad
        )
        try:
            response = self.stub.registrarDonacion(request)
            return response
        except grpc.RpcError as e:
            print(f"Error al registrar donación: {e.code()} - {e.details()}")
            return None

    #Lista de las donaciones
    def listar_donaciones(self, token):
        self.connect()
        request = ListarDonacionesRequest(token=token)
        try:
            response = self.stub.listarDonaciones(request)
            return response
        except grpc.RpcError as e:
            print(f"Error al listar donaciones: {e.code()} - {e.details()}")
            return None
        
    # Elimina donación
    def eliminar_donacion(self, token, donacion_id):
        self.connect()
        request = EliminarDonacionRequest(
            token=token,
            id=donacion_id
        )
        try:
            response = self.stub.eliminarDonacion(request)
            return response
        except grpc.RpcError as e:
            print(f"Error al eliminar donación: {e.code()} - {e.details()}")
            return None

    # Modifica donacion
    def modificar_donacion(self, token, donacion_id, descripcion, cantidad):
        self.connect()
        request = ModificarDonacionRequest(
            token=token,
            id=donacion_id,
            descripcion=descripcion,
            cantidad=cantidad
        )
        try:
            response = self.stub.modificarDonacion(request)
            return response
        except grpc.RpcError as e:
            print(f"Error al modificar donación: {e.code()} - {e.details()}")
            return None

    # Trae donación por su ID
    def traer_donacion_por_id(self, token, donacion_id):
        self.connect()
        request = DonacionIdRequest(
            token=token,
            id=donacion_id
        )
        try:
            response = self.stub.traerDonacionPorId(request)
            return response
        except grpc.RpcError as e:
            print(f"Error al obtener donación: {e.code()} - {e.details()}")
            return None
        
    def cerrar(self):
        if self.channel is not None:
            self.channel.close()
            self.channel = None
            self.stub = None