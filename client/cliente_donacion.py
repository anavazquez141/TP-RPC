from datetime import datetime
import grpc
import requests
from flask import flash
from proto import donacionService_pb2 as donacion_pb2
from proto import donacionService_pb2_grpc as donacion_pb2_grpc

class ClienteDonacion:
    def __init__(self, host='localhost', port=9090):
        self.host = host
        self.port = port
        self.channel = None
        self.stub = None

    def connect(self):
        if self.channel is None or self.is_channel_closed():
            self.channel = grpc.insecure_channel(f"{self.host}:{self.port}")
            # CORRECCIÓN: Usa DonacionServiceStub en lugar de llamar al módulo directamente
            self.stub = donacion_pb2_grpc.DonacionServiceStub(self.channel)

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
        request = donacion_pb2.RegistrarDonacionRequest(
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
        request = donacion_pb2.ListarDonacionesRequest(token=token)
        try:
            response = self.stub.listarDonaciones(request)
            return response
        except grpc.RpcError as e:
            print(f"Error al listar donaciones: {e.code()} - {e.details()}")
            return None
        
    # Elimina donación
    def eliminar_donacion(self, token, donacion_id):
        self.connect()
        request = donacion_pb2.EliminarDonacionRequest(
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
        request = donacion_pb2.ModificarDonacionRequest(
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
        request = donacion_pb2.DonacionIdRequest(
            token=token,
            id=donacion_id
        )
        try:
            response = self.stub.traerDonacionPorId(request)
            return response
        except grpc.RpcError as e:
            print(f"Error al obtener donación: {e.code()} - {e.details()}")
            return None

    def baja_solicitud_donacion(self, token, id_organizacion, id_solicitud):
        self.connect()
        request = donacion_pb2.BajaSolicitudRequest(
            token=token,
            id_organizacion=id_organizacion,
            id_solicitud=id_solicitud
        )
        try:
            response = self.stub.bajaSolicitudDonacion(request)
            return response
        except grpc.RpcError as e:
            print(f"Error al dar de baja solicitud: {e.code()} - {e.details()}")
            return None
        
    def listar_solicitudes(self, token):
        self.connect()
        request = donacion_pb2.ListarSolicitudesRequest(token=token)
        try:
            response = self.stub.listarSolicitudes(request)
            return response
        except grpc.RpcError as e:
            print(f"Error al listar solicitudes: {e.code()} - {e.details()}")
            return None
        
    def solicitar_donacion(self, id_organizacion, id_solicitud, items):
        self.connect()  # Asegura que el canal y stub estén inicializados
        if not id_organizacion or not id_solicitud:
            print("Error: ID de organización y solicitud no pueden estar vacíos")
            return None
        if not items:
            print("Error: Debe haber al menos un ítem")
            return None
        
        request = donacion_pb2.SolicitarDonacionRequest(
            token="",  # vacío, no requiere autenticación
            id_organizacion=id_organizacion,
            id_solicitud=id_solicitud,
            items=items
        )
        try:
            response = self.stub.solicitarDonacion(request, timeout=10)
            print(f"Respuesta de solicitar_donacion: status={response.status}, message={response.message}")
            return response
        except grpc.RpcError as e:
            print(f"Error gRPC al solicitar donación: {e.code().name} - {e.details()}")
            return None
        except Exception as e:
            print(f"Error inesperado al solicitar donación: {str(e)}")
            return None
        
    def cerrar(self):
        if self.channel is not None:
            self.channel.close()
            self.channel = None
            self.stub = None

    def listar_ofertas(self, token):
        self.connect()
        request = donacion_pb2.ListarOfertasRequest(token=token)
        try:
            response = self.stub.listarOfertas(request)
            return response
        except grpc.RpcError as e:
            print(f"Error al listar ofertas: {e.code()} - {e.details()}")
            return None        

    def ofrecer_donacion(self, id_organizacion, id_oferta, items):
        self.connect()  # Asegura que el canal y stub estén inicializados

        # Validaciones simples
        if not id_organizacion:
            print("Error: ID de organización no puede estar vacío")
            return None

        if not id_oferta:
            print("Error: ID de oferta no puede estar vacío")
            return None  

        if not items:
            print("Error: Debe haber al menos un ítem")
            return None

        # Crear request gRPC
        request = donacion_pb2.OfertaDonacionRequest(
            token="",  # vacío, si no requiere autenticación
            idOferta=id_oferta,
            idOrganizacion=id_organizacion,
            items=items
        )

        try:
            response = self.stub.ofrecerDonacion(request, timeout=10)
            print(f"Respuesta de ofrecer_donacion: status={response.status}, message={response.message}")
            return response
        except grpc.RpcError as e:
            print(f"Error gRPC al ofrecer donación: {e.code().name} - {e.details()}")
            return None
        except Exception as e:
            print(f"Error inesperado al ofrecer donación: {str(e)}")
            return None            
        
    def informe_donaciones(self, token):
        self.connect()
        request = donacion_pb2.ListarDonacionesRequest(token=token)
        try:
            response = self.stub.listarDonacionesConEliminado(request)
            return response
        except grpc.RpcError as e:
            print(f"Error al obtener informe: {e.code()} - {e.details()}")
            return None
    
