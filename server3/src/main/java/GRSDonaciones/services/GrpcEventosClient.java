package GRSDonaciones.services;

import GRSDonaciones.grpc.EventoSolidarioServiceProto;
import GRSDonaciones.grpc.EventoSolidarioServiceProto.ListarParticipacionesRequest;
import GRSDonaciones.grpc.EventoSolidarioServiceProto.ListarParticipacionesResponse;
import GRSDonaciones.grpc.EventosServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrpcEventosClient {

    private static final Logger logger = LoggerFactory.getLogger(GrpcEventosClient.class);

    @GrpcClient("eventos-service") // Ajustá el nombre del cliente gRPC en application.yml
    private EventosServiceGrpc.EventosServiceBlockingStub stub;

    public List<EventoSolidarioServiceProto.ParticipacionEvento> listarParticipacionesEventos(String token) {
        ListarParticipacionesRequest request = ListarParticipacionesRequest.newBuilder()
                .setToken(token)
                .build();

        ListarParticipacionesResponse response = stub.listarParticipacionesEventos(request);

        if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
            throw new RuntimeException("Error gRPC EventosService: " + response.getMessage());
        }

        return response.getParticipacionesList();
    }
}