package RpcDonaciones.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import RpcDonaciones.grpc.DonacionServiceGrpc;
import RpcDonaciones.grpc.DonacionServiceProto.*;
import RpcDonaciones.repositories.IDonacion;
import RpcDonaciones.entities.Donacion;
import RpcDonaciones.entities.enums.CategoriaDonacion;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.entities.Usuario;

import java.util.List;
import java.util.Optional;

@Service
public class DonacionServiceImpl extends DonacionServiceGrpc.DonacionServiceImplBase {

    @Autowired
    private IDonacion donacionRepository;
    
    @Autowired
    private AuthServiceImpl authService;
    @Autowired
    private final TokenValidator tokenValidator = null;

    @Override
    public void registrarDonacion(RegistrarDonacionRequest request, 
                                StreamObserver<DonacionResponse> responseObserver) {
        try {
            // Validacion de token y donacion
            if (!tokenValidator.isTokenValid(request.getToken())) {
                sendErrorResponse(responseObserver, "Token inválido o expirado");
                return;
            }
        
            // Verificar la donacion
            if (request.getCantidad() <= 0) {
                sendErrorResponse(responseObserver, "La cantidad debe ser mayor a 0");
                return;
            }
            
            // Crear y persistir la donacion
            Donacion donacion = new Donacion();
            donacion.setCategoria(CategoriaDonacion.fromString(request.getCategoria()));
            donacion.setDescripcion(request.getDescripcion());
            donacion.setCantidad(request.getCantidad());
            
            // Guarda la donacion en la base de datos
            Donacion saved = donacionRepository.save(donacion);
            
            //Envia respuesta
            DonacionResponse response = DonacionResponse.newBuilder()
                .setId(saved.getId())
                .setCategoria(saved.getCategoria().name())
                .setDescripcion(saved.getDescripcion())
                .setCantidad(saved.getCantidad())
                .setStatus("SUCCESS")
                .setMessage("Donación registrada exitosamente")
                .build();
                
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            sendErrorResponse(responseObserver, "Error al registrar donación: " + e.getMessage());
        }
    }

    @Override
    public void listarDonaciones(ListarDonacionesRequest request,
                               StreamObserver<ListarDonacionesResponse> responseObserver) {
        try {
            if (!tokenValidator.isTokenValid(request.getToken())) {
                sendErrorResponse(responseObserver, "Token inválido o expirado");
                return;
            }
            
            List<Donacion> donaciones = donacionRepository.findByEliminadoFalse();
            
            ListarDonacionesResponse.Builder responseBuilder = ListarDonacionesResponse.newBuilder();
            
            for (Donacion donacion : donaciones) {
                DonacionItem item = DonacionItem.newBuilder()
                    .setId(donacion.getId())
                    .setCategoria(donacion.getCategoria().name())
                    .setDescripcion(donacion.getDescripcion())
                    .setCantidad(donacion.getCantidad())
                    .build();
                responseBuilder.addDonaciones(item);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            sendErrorResponse(responseObserver, "Error al listar donaciones: " + e.getMessage());
        }
    }

    @Override
    public void eliminarDonacion(EliminarDonacionRequest request,
                               StreamObserver<EliminarDonacionResponse> responseObserver) {
        try {
            if (!tokenValidator.isTokenValid(request.getToken())) {
                sendErrorResponse(responseObserver, "Token inválido o expirado");
                return;
            }
            
            Optional<Donacion> donacionOpt = donacionRepository.findById(request.getId());
            if (donacionOpt.isPresent()) {
                Donacion donacion = donacionOpt.get();
                donacion.setEliminado(true);
                donacionRepository.save(donacion);
                
                EliminarDonacionResponse response = EliminarDonacionResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Donación eliminada exitosamente")
                    .build();
                responseObserver.onNext(response);
            } else {
                EliminarDonacionResponse response = EliminarDonacionResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Donación no encontrada")
                    .build();
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error al eliminar donación: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void traerDonacionPorId(DonacionIdRequest request,
                                StreamObserver<DonacionResponse> responseObserver) {
        try {
            // Validar token
            if (!tokenValidator.isTokenValid(request.getToken())) {
                sendErrorResponse(responseObserver, "Token inválido o expirado");
                return;
            }

            // Buscar donación por ID
            Optional<Donacion> donacionOpt = donacionRepository.findById(request.getId());

            if (donacionOpt.isPresent() && !donacionOpt.get().isEliminado()) {
                Donacion donacion = donacionOpt.get();

                DonacionResponse response = DonacionResponse.newBuilder()
                    .setId(donacion.getId())
                    .setCategoria(donacion.getCategoria().name())
                    .setDescripcion(donacion.getDescripcion())
                    .setCantidad(donacion.getCantidad())
                    .setStatus("SUCCESS")
                    .setMessage("Donación encontrada correctamente")
                    .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                DonacionResponse response = DonacionResponse.newBuilder()
                    .setStatus("ERROR")
                    .setMessage("Donación no encontrada o eliminada")
                    .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }

        } catch (Exception e) {
            DonacionResponse response = DonacionResponse.newBuilder()
                .setStatus("ERROR")
                .setMessage("Error al obtener la donación: " + e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }


    private void sendErrorResponse(StreamObserver<?> responseObserver, String message) {
        responseObserver.onError(Status.INTERNAL
            .withDescription(message)
            .asRuntimeException());
    }
}
