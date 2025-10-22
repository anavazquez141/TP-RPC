package GRSDonaciones.services;

import GRSDonaciones.grpc.AuthServiceGrpc;
import GRSDonaciones.grpc.AuthServiceProto;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class GrpcAuthClient {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub stub;

    public boolean validarToken(String token) {
        AuthServiceProto.TokenValidationRequest request =
                AuthServiceProto.TokenValidationRequest.newBuilder()
                        .setToken(token)
                        .build();

        AuthServiceProto.TokenValidationResponse response = stub.validarToken(request);
        return "SUCCESS".equalsIgnoreCase(response.getStatus());
    }

    public String login(String email, String clave) {
        AuthServiceProto.LoginRequest request =
                AuthServiceProto.LoginRequest.newBuilder()
                        .setEmail(email)
                        .setClave(clave)
                        .build();

        AuthServiceProto.LoginResponse response = stub.login(request);
        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            return response.getToken();
        }
        throw new RuntimeException(response.getMessage());
    }
}
