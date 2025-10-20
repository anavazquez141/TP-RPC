package GRSDonaciones.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import GRSDonaciones.grpc.AuthServiceGrpc;
import GRSDonaciones.grpc.AuthServiceProto.LoginRequest;
import GRSDonaciones.grpc.AuthServiceProto.LoginResponse;
import GRSDonaciones.grpc.DonacionServiceGrpc;

@Configuration
public class GrpcClientConfig {

    private final ManagedChannel channel;

    public GrpcClientConfig(@Value("${grpc.server1.host:localhost}") String host,
                           @Value("${grpc.server1.port:9090}") int port) {
        System.out.println("gRPC host: " + host + ", port: " + port);
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                                           .usePlaintext()
                                           .build();

        // Prueba de conexión (opcional, considera moverla a un entorno de desarrollo)
        try {
            AuthServiceGrpc.AuthServiceBlockingStub authStub = AuthServiceGrpc.newBlockingStub(channel);
            LoginRequest request = LoginRequest.newBuilder()
                .setEmail("test@example.com")
                .setClave("password123")
                .build();
            LoginResponse response = authStub.login(request);
            System.out.println("Prueba de conexión gRPC exitosa: " + response);
        } catch (Exception e) {
            System.err.println("Error en la prueba de conexión gRPC: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Bean
    public DonacionServiceGrpc.DonacionServiceBlockingStub donacionServiceStub() {
        return DonacionServiceGrpc.newBlockingStub(channel);
    }

    @Bean
    public AuthServiceGrpc.AuthServiceBlockingStub authServiceStub() {
        return AuthServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        System.out.println("Cerrando canal gRPC...");
        channel.shutdown();
        if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
            System.out.println("El canal gRPC no se cerró en 5 segundos, forzando cierre...");
            channel.shutdownNow();
        }
    }
}