import requests
from flask import current_app

class ClienteEventoGraphQL:
    def __init__(self):
        self.endpoint = "http://localhost:8050/graphql"  # Ajustá si es diferente

    def informe_participacion(self, token, usuario, fecha_desde=None, fecha_hasta=None):
        query = """
        query Informe($filtro: FiltroParticipacionInput!, $token: String!) {
            informeParticipacionEventos(filtro: $filtro, token: $token) {
                mes
                dia
                nombreEvento
                descripcion
            }
        }
        """
        variables = {
            "filtro": {
                "usuario": usuario,
                "fechaDesde": fecha_desde,
                "fechaHasta": fecha_hasta
            },
            "token": token
        }
        response = requests.post(
            self.endpoint,
            json={"query": query, "variables": variables},
            headers={"Authorization": f"Bearer {token}"}
        )
        data = response.json()
        if "errors" in data:
            raise Exception(data["errors"])
        return data["data"]["informeParticipacionEventos"]

    def obtener_usuarios(self, token):
        query = """
        query {
            usuarios {
                username
            }
        }
        """
        headers = {"Authorization": f"Bearer {token}"}
        response = requests.post(self.endpoint, json={"query": query}, headers=headers)
        data = response.json()
        if "errors" in data:
            raise Exception(data["errors"])
        return [u["username"] for u in data["data"]["usuarios"]]