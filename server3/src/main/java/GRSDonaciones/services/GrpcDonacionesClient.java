package GRSDonaciones.services;

import GRSDonaciones.grpc.DonacionServiceGrpc;
import GRSDonaciones.grpc.DonacionServiceProto;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrpcDonacionesClient {

    @GrpcClient("donaciones-service")
    private DonacionServiceGrpc.DonacionServiceBlockingStub stub;

    public List<DonacionServiceProto.DonacionConCampoEliminado> listarDonacionesConEliminado(String token) {
        DonacionServiceProto.ListarDonacionesRequest request =
                DonacionServiceProto.ListarDonacionesRequest.newBuilder()
                        .setToken(token)
                        .build();

        DonacionServiceProto.ListarDonacionesConEliminadoResponse response = stub.listarDonacionesConEliminado(request);

        if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
            throw new RuntimeException("Error gRPC DonacionService: " + response.getMessage());
        }

        return response.getDonacionesList();
    }
}
