import requests

class ClienteDonacionGraph:
    def __init__(self):
        self.endpoint = "http://localhost:8050/graphql"

    def informe_donaciones_filtradas(self, token, categoria=None, fecha_desde=None, fecha_hasta=None, eliminado=None):
        query = """
        query InformeDonaciones($filtro: FiltroDonacionInput,  $token: String!) {
            informeDonaciones(filtro: $filtro, token: $token) {
                categoria
                eliminado
                totalCantidad
            }
        }
        """

        variables = {
            "filtro": {
                "categoria": categoria,
                "fechaDesde": fecha_desde,
                "fechaHasta": fecha_hasta,
                "eliminado": eliminado
            },
            "token": token
        }

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}"  # 🔹 Agregamos el header JWT
        }

        response = requests.post(
            self.endpoint,
            json={"query": query, "variables": variables},
            headers=headers
        )

        data = response.json()

        # Log útil si querés debuggear
        print("📡 Respuesta GraphQL:", data)

        if "errors" in data:
            raise Exception(f"Errores en GraphQL: {data['errors']}")

        if "data" not in data or data["data"]["informeDonaciones"] is None:
            raise Exception("No se recibió información del servidor GraphQL")

        return data["data"]["informeDonaciones"]
    

