import grpc
from proto import eventoSolidarioService_pb2 as eventoSolidario_pb2
from proto import eventoSolidarioService_pb2_grpc as eventoSolidario_pb2_grpc
from datetime import datetime
import sys


class ClienteEvento:
    def __init__(self, base_url="localhost"):
        self.base_url = base_url
        self.headers = {"Content-Type": "application/json"}
        self.host = "localhost"
        self.port = 9090
        self.channel = None
        self.evento_stub = None

    def create_evento(self, nombre_evento, descripcion, fecha_hora, usuario_ids=None):
        #payload = {
        #    "nombreEvento": nombre_evento,
        #    "descripcion": descripcion,
        #    "fechaHora": fecha_hora,
        #    "usuarioIds": usuario_ids or []
        #}


        self.connect()
        request = eventoSolidario_pb2.CreateEventoRequest(
            nombreEvento=nombre_evento,
            descripcion=descripcion,
            fecha_hora=fecha_hora,
            usuarioIds=usuario_ids or []
        )
        try:
            response = self.evento_stub.CreateEvento(request)
            print(f"Respuesta de registrarEvento: {response}")
            return response
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")
            return None
        

        #response = requests.post(self.base_url, json=payload, headers=self.headers)
        #response.raise_for_status()
        #return response.json()

    def get_evento(self, id_evento):
        response = requests.get(f"{self.base_url}/{id_evento}", headers=self.headers)
        response.raise_for_status()
        return response.json()

    def list_eventos(self):
        response = requests.get(self.base_url, headers=self.headers)
        response.raise_for_status()
        return response.json()

    def update_evento(self, id_evento, nombre_evento=None, descripcion=None, fecha_hora=None, usuario_ids=None):
        payload = {}
        if nombre_evento:
            payload["nombreEvento"] = nombre_evento
        if descripcion:
            payload["descripcion"] = descripcion
        if fecha_hora:
            payload["fechaHora"] = fecha_hora
        if usuario_ids is not None:
            payload["usuarioIds"] = usuario_ids
        response = requests.post(f"{self.base_url}/{id_evento}", json=payload, headers=self.headers)
        response.raise_for_status()
        return response.json()

    def delete_evento(self, id_evento):
        response = requests.post(f"{self.base_url}/{id_evento}/delete", headers=self.headers)
        response.raise_for_status()
        return True