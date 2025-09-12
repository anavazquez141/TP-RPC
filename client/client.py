import grpc
import authService_pb2  
import authService_pb2_grpc  

def login(email, password):
    with grpc.insecure_channel('localhost:9090') as channel:
        stub = authService_pb2_grpc.AuthServiceStub(channel) 
        request = authService_pb2.LoginRequest(email=email, clave=password)  
        try:
            response = stub.login(request)
            print(f"Status: {response.status}")
            print(f"Message: {response.message}")
            if response.status == "SUCCESS":
                print(f"Token: {response.token}")
                with open('token.txt', 'w') as f:
                    f.write(response.token)
            return response.token
        except grpc.RpcError as e:
            print(f"Error de gRPC: {e.code()} - {e.details()}")  # Imprime más detalles del error
            return None
        except Exception as e:
            print(f"Error inesperado: {str(e)}")  # Imprime detalles de otros errores
            return None
        
def logout(token):
    with grpc.insecure_channel('localhost:9090') as channel:
        stub = authService_pb2_grpc.AuthServiceStub(channel) 
        request = authService_pb2.LogoutRequest(token=token)
        response = stub.logout(request)
        print(f"Status: {response.status}")
        print(f"Message: {response.message}")
        if response.status == "SUCCESS":
            print("Cierre de sesión exitoso!")

def main():
    token = login("test@example.com", "password123")
    if token:
        print("Inicio de sesión exitoso!")
    else:
        print("Fallo en el inicio de sesión.")
    if token:
        logout(token)
        print("Sesion cerrada satisfactoriamente.")

if __name__ == '__main__':
    main()