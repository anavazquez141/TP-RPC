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
    
# ------------------ Filtros guardados ------------------

    def guardar_filtro(self, token, nombre, categoria=None, fecha_desde=None, fecha_hasta=None, eliminado=None):
        mutation = """
        mutation GuardarFiltro($input: FiltroInput!, $token: String!) {
            guardarFiltro(filtro: $input, token: $token) {
                id
                nombreFiltro
            }
        }
        """
        variables = {
            "input": {
                "nombreFiltro": nombre,
                "categoria": categoria,
                "fechaDesde": fecha_desde,
                "fechaHasta": fecha_hasta,
                "eliminado": eliminado
            },
            "token": token
        }
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}"
        }
        response = requests.post(self.endpoint, json={"query": mutation, "variables": variables}, headers=headers)
        data = response.json()
        if "errors" in data:
            raise Exception(f"Errores al guardar filtro: {data['errors']}")
        return data["data"]["guardarFiltro"]

    def traer_filtros(self, token):
        query = """
        query TraerFiltros($token: String!) {
            filtrosGuardados(token: $token) {
                id
                nombreFiltro
                categoria
                fechaDesde
                fechaHasta
                eliminado
            }
        }
        """
        variables = {"token": token}
        headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
        response = requests.post(self.endpoint, json={"query": query, "variables": variables}, headers=headers)
        data = response.json()
        if "errors" in data:
            raise Exception(f"Error al traer filtros: {data['errors']}")
        return data["data"]["filtrosGuardados"]

    def traer_filtro_por_id(self, token, filtro_id):
        filtros = self.traer_filtros(token)
        for f in filtros:
            if str(f["id"]) == str(filtro_id):
                return f
        raise Exception("Filtro no encontrado")

    def eliminar_filtro(self, token, filtro_id):
        mutation = """
        mutation EliminarFiltro($id: ID!, $token: String!) {
            eliminarFiltro(id: $id, token: $token) {
                success
            }
        }
        """
        variables = {"id": filtro_id, "token": token}
        headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
        response = requests.post(self.endpoint, json={"query": mutation, "variables": variables}, headers=headers)
        data = response.json()
        if "errors" in data:
            raise Exception(f"Error al eliminar filtro: {data['errors']}")
        return data["data"]["eliminarFiltro"]["success"]   

