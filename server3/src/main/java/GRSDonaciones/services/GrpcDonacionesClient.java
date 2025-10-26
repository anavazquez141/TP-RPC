package GRSDonaciones.services;

import GRSDonaciones.controllers.InformeDonacionesController;
import GRSDonaciones.grpc.DonacionServiceGrpc;
import GRSDonaciones.grpc.DonacionServiceProto;
import net.devh.boot.grpc.client.inject.GrpcClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrpcDonacionesClient {

    private static final Logger logger = LoggerFactory.getLogger(InformeDonacionesController.class);

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

   public DonacionServiceProto.ListarDonacionesParaExcelResponse listarDonacionesParaExcelClient(
            DonacionServiceProto.ListarDonacionesParaExcelRequest request) {
        logger.info("Llamando a listarDonacionesParaExcel con token: {}, categoria: {}, fechaDesde: {}, fechaHasta: {}, eliminado: {}",
                request.getToken(), request.getCategoria(), request.getFechaDesde(), request.getFechaHasta(), request.getEliminado());

        try {
            DonacionServiceProto.ListarDonacionesParaExcelResponse response =
                    stub.listarDonacionesParaExcel(request);

            if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
                logger.error("Error en gRPC DonacionService (Excel): {}", response.getMessage());
                throw new RuntimeException("Error gRPC DonacionService (Excel): " + response.getMessage());
            }

            logger.info("Respuesta gRPC recibida con {} donaciones", response.getDonacionesList().size());
            return response; // Devolver el objeto completo
        } catch (Exception e) {
            logger.error("Error al llamar a listarDonacionesParaExcel", e);
            throw new RuntimeException("Error al llamar a gRPC DonacionService: " + e.getMessage());
        }
    }
}
