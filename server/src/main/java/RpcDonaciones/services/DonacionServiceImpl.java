package RpcDonaciones.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import RpcDonaciones.grpc.DonacionServiceGrpc;
import RpcDonaciones.grpc.DonacionServiceProto.*;
import RpcDonaciones.kafka.messages.BajaSolicitudMessage;
import RpcDonaciones.kafka.messages.SolicitudDonacionMessage;
import RpcDonaciones.kafka.messages.SolicitudDonacionMessage.ItemDonacionM;
import RpcDonaciones.kafka.messages.TransferenciaDonacionMessage;
import RpcDonaciones.kafka.producers.KafkaProducerService;
import RpcDonaciones.repositories.IAuditoria;
import RpcDonaciones.repositories.IOfertaSolicitud;
import RpcDonaciones.repositories.IDonacion;
import RpcDonaciones.repositories.ISolicitudDonacion;
import RpcDonaciones.entities.Auditoria;
import RpcDonaciones.entities.BajaSolicitud;
import RpcDonaciones.entities.Donacion;
import RpcDonaciones.entities.SolicitudDonacion;
import RpcDonaciones.entities.enums.CategoriaDonacion;
import RpcDonaciones.entities.enums.TipoAccion;
import RpcDonaciones.entities.OfertaSolicitud;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.repositories.IBajaSolicitud;
import RpcDonaciones.entities.Usuario;

import RpcDonaciones.entities.ItemDonacion;

import RpcDonaciones.kafka.messages.OfertaDonacionMessage;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

@Service
public class DonacionServiceImpl extends DonacionServiceGrpc.DonacionServiceImplBase {

    @Autowired
    private IDonacion donacionRepository;
    @Autowired
    private IAuditoria auditoriaRepository;
    @Autowired
    private AuthServiceImpl authService;
    @Autowired
    private KafkaProducerService kafkaProducerService;
    @Autowired
    private ISolicitudDonacion solicitudDonacionRepository;
    @Autowired
    private IBajaSolicitud bajaSolicitudRepository;

    @Autowired
    private IOfertaSolicitud ofertaSolicitudRepository;

    private final TokenValidator tokenValidator;
    private final SecretKey key;

    public DonacionServiceImpl(@Value("${jwt.secret}") String base64Key, TokenValidator tokenValidator) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key));
        this.tokenValidator = tokenValidator;
    }

    @Override
    public void registrarDonacion(RegistrarDonacionRequest request, StreamObserver<DonacionResponse> responseObserver) {
        try {
            String token = request.getToken();
            if (!tokenValidator.validarToken(token, responseObserver, "UsuarioResponse")) {
                return;
            }

            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseClaimsJws(token)
                .getPayload();

            List<String> roles = claims.get("roles", List.class);
                if (!roles.contains("ROLE_PRESIDENTE") && !roles.contains("ROLE_VOCAL")) {
                responseObserver.onNext(DonacionResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("No tienes permiso para modificar eventos")
                        .build());
                responseObserver.onCompleted();
                return;
            }
        
            // Verificar la donacion
            if (request.getCantidad() <= 0) {
                sendErrorResponse(responseObserver, "La cantidad debe ser mayor a 0");
                return;
            }
            
            Usuario usuario = authService.getUsuarioFromToken(request.getToken());
            // Crear y persistir la donacion
            Donacion donacion = new Donacion();
            donacion.setCategoria(CategoriaDonacion.fromString(request.getCategoria()));
            donacion.setDescripcion(request.getDescripcion());
            donacion.setCantidad(request.getCantidad());
            donacion.setUsuarioAlta(usuario.getNombreUsuario());
            donacion.setFechaAlta(java.time.LocalDateTime.now());
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
    public void listarDonacionesConEliminado(ListarDonacionesRequest request, 
                                            StreamObserver<ListarDonacionesConEliminadoResponse> responseObserver) {
        System.out.println("=== Ingresó a listarDonacionesConEliminado ===");
        System.out.println("Request recibido: " + request);

        try {
            String token = request.getToken();
            System.out.println("Token recibido: " + token);

            if (!tokenValidator.validarToken(token, responseObserver, "UsuarioResponse")) {
                System.out.println("Token inválido, terminando ejecución");
                return;
            }

            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseClaimsJws(token)
                .getPayload();

            List<String> roles = claims.get("roles", List.class);
            if (!roles.contains("ROLE_PRESIDENTE") && !roles.contains("ROLE_VOCAL")) {
                responseObserver.onNext(ListarDonacionesConEliminadoResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("No tienes permiso para ver informe de donaciones")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            List<Donacion> donaciones = donacionRepository.findAll();
            System.out.println("Cantidad de donaciones obtenidas: " + donaciones.size());

            ListarDonacionesConEliminadoResponse.Builder response = ListarDonacionesConEliminadoResponse.newBuilder();

            for (Donacion d : donaciones) {
                String fechaEliminacion = "";
                if (d.isEliminado()) {
                    fechaEliminacion = d.getAuditorias().stream()
                        .filter(a -> a.getTipoAccion() == TipoAccion.ELIMINACION)
                        .map(a -> a.getFecha().toString())
                        .findFirst()
                        .orElse("");
                }

                DonacionConCampoEliminado item = DonacionConCampoEliminado.newBuilder()
                    .setId(d.getId())
                    .setCategoria(d.getCategoria().name())
                    .setDescripcion(d.getDescripcion())
                    .setCantidad(d.getCantidad())
                    .setEliminado(d.isEliminado())
                    .setFechaAlta(d.getFechaAlta().toString())
                    .setFechaEliminacion(fechaEliminacion)
                    .build();

                response.addDonaciones(item);
            }

            response.setStatus("SUCCESS");
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();

            System.out.println("Respuesta enviada correctamente al cliente gRPC");
        } catch (Exception e) {
            System.out.println("Error en listarDonacionesConEliminado: " + e.getMessage());
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    @Override
    public void modificarDonacion(ModificarDonacionRequest request, StreamObserver<DonacionResponse> responseObserver) {
        try {
            String token = request.getToken();
            if (!tokenValidator.validarToken(token, responseObserver, "UsuarioResponse")) {
                return;
            }

            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseClaimsJws(token)
                .getPayload();

            List<String> roles = claims.get("roles", List.class);
                if (!roles.contains("ROLE_PRESIDENTE") && !roles.contains("ROLE_VOCAL")) {
                responseObserver.onNext(DonacionResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("No tienes permiso para modificar eventos")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            Optional<Donacion> optional = donacionRepository.findById(request.getId());
            if (optional.isEmpty() || optional.get().isEliminado()) {
                sendErrorResponse(responseObserver, "Donación no encontrada o eliminada");
                return;
            }

            Donacion donacion = optional.get();
            Usuario usuario = authService.getUsuarioFromToken(request.getToken());

            // Guardar auditorías solo si cambia algo
            if (!donacion.getCantidad().equals(request.getCantidad())) {
                guardarAuditoria(donacion, usuario, "cantidad", 
                    donacion.getCantidad().toString(), 
                    String.valueOf(request.getCantidad()), 
                    TipoAccion.MODIFICACION);
                donacion.setCantidad(request.getCantidad());
            }

            if (!donacion.getDescripcion().equals(request.getDescripcion())) {
                guardarAuditoria(donacion, usuario, "descripcion", 
                    donacion.getDescripcion(), 
                    request.getDescripcion(), 
                    TipoAccion.MODIFICACION);
                donacion.setDescripcion(request.getDescripcion());
            }

            donacionRepository.save(donacion);

            DonacionResponse response = DonacionResponse.newBuilder()
                .setId(donacion.getId())
                .setCategoria(donacion.getCategoria().name())
                .setDescripcion(donacion.getDescripcion())
                .setCantidad(donacion.getCantidad())
                .setStatus("SUCCESS")
                .setMessage("Donación modificada exitosamente")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            sendErrorResponse(responseObserver, "Error al modificar donación: " + e.getMessage());
        }
    }


    @Override
    public void eliminarDonacion(EliminarDonacionRequest request, StreamObserver<EliminarDonacionResponse> responseObserver) {
        try {
            String token = request.getToken();
            if (!tokenValidator.validarToken(token, responseObserver, "UsuarioResponse")) {
                return;
            }

            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseClaimsJws(token)
                .getPayload();

            List<String> roles = claims.get("roles", List.class);
                if (!roles.contains("ROLE_PRESIDENTE") && !roles.contains("ROLE_VOCAL")) {
                responseObserver.onNext(EliminarDonacionResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("No tienes permiso para modificar eventos")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            Optional<Donacion> optional = donacionRepository.findById(request.getId());
            if (optional.isEmpty()) {
                responseObserver.onNext(EliminarDonacionResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Donación no encontrada")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            Donacion donacion = optional.get();
            if (donacion.isEliminado()) {
                responseObserver.onNext(EliminarDonacionResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("La donación ya estaba eliminada")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            Usuario usuario = authService.getUsuarioFromToken(request.getToken());
            donacion.setEliminado(true);
            donacionRepository.save(donacion);

            guardarAuditoria(donacion, usuario, null, null, null, TipoAccion.ELIMINACION);

            responseObserver.onNext(EliminarDonacionResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Donación dada de baja lógicamente")
                .build());
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

    private void guardarAuditoria(Donacion donacion, Usuario usuario, String campoModificado, String valorAnterior, String valorNuevo, TipoAccion tipoAccion) {
        if (donacion == null || usuario == null) {
        throw new IllegalArgumentException("Donacion o Usuario no pueden ser nulos");
    }
        Auditoria auditoria = new Auditoria();
        auditoria.setDonacion(donacion);
        auditoria.setUsuario(usuario.getNombreUsuario());
        auditoria.setFecha(LocalDateTime.now());
        auditoria.setTipoAccion(tipoAccion);

        if (tipoAccion == TipoAccion.ELIMINACION) {
            auditoria.setCampoModificado("eliminado");
            auditoria.setValorAnterior("false");
            auditoria.setValorNuevo("true");
        } else if (tipoAccion == TipoAccion.MODIFICACION) {
            auditoria.setCampoModificado(campoModificado);
            auditoria.setValorAnterior(valorAnterior);
            auditoria.setValorNuevo(valorNuevo);
        }

        // Agregar la auditoría a la lista de la donación
        donacion.getAuditorias().add(auditoria);
        auditoriaRepository.save(auditoria);
        // Opcional: Guardar la donación para persistir la relación
        donacionRepository.save(donacion);
    }

    private void sendErrorResponse(StreamObserver<?> responseObserver, String message) {
        responseObserver.onError(Status.INTERNAL
            .withDescription(message)
            .asRuntimeException());
    }

    @Override
    public void solicitarDonacion(SolicitarDonacionRequest request, StreamObserver<SolicitarDonacionResponse> responseObserver) {
        try {
            // Validaciones
            if (request.getIdOrganizacion().isEmpty() || request.getIdSolicitud().isEmpty()) {
                responseObserver.onNext(SolicitarDonacionResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("ID de organización y solicitud son obligatorios")
                    .build());
                responseObserver.onCompleted();
                return;
            }
            if (request.getItemsList().isEmpty()) {
                responseObserver.onNext(SolicitarDonacionResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Debe haber al menos un ítem")
                    .build());
                responseObserver.onCompleted();
                return;
            }
            if (solicitudDonacionRepository.existsByIdSolicitud(request.getIdSolicitud())) {
                responseObserver.onNext(SolicitarDonacionResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Ya existe una solicitud con ese ID")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            // Construir mensaje Kafka
            SolicitudDonacionMessage message = new SolicitudDonacionMessage();
            message.setIdOrganizacion(request.getIdOrganizacion());
            message.setIdSolicitud(request.getIdSolicitud());
            message.setDonaciones(request.getItemsList().stream()
                .map(item -> new SolicitudDonacionMessage.ItemDonacionM(item.getCategoria(), item.getDescripcion()))
                .collect(Collectors.toList()));
            kafkaProducerService.sendSolicitudDonacion(message);

            System.out.println("Solicitud enviada a Kafka: " + request.getIdSolicitud());

            responseObserver.onNext(SolicitarDonacionResponse.newBuilder()
                .setStatus("SUCCESS")
                .setMessage("Solicitud enviada correctamente a Kafka")
                .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            System.out.println("Error al procesar solicitud: " + e.getMessage());
            responseObserver.onError(
                Status.INTERNAL.withDescription("Error al procesar la solicitud: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void bajaSolicitudDonacion(BajaSolicitudRequest request, StreamObserver<BajaSolicitudResponse> responseObserver) {
        try {
            if (!tokenValidator.validarToken(request.getToken(), responseObserver, "BajaSolicitudResponse")) {
                return;
            }
            // Validar roles (PRESIDENTE o VOCAL)
            Claims claims = Jwts.parser().verifyWith(key).build().parseClaimsJws(request.getToken()).getPayload();
            List<String> roles = claims.get("roles", List.class);
            if (!roles.contains("ROLE_PRESIDENTE") && !roles.contains("ROLE_VOCAL")) {
                responseObserver.onNext(BajaSolicitudResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("No tienes permiso para dar de baja solicitudes")
                    .build());
                responseObserver.onCompleted();
                return;
            }
            // Buscar la solicitud
            Optional<SolicitudDonacion> solicitudOpt = solicitudDonacionRepository.findByIdOrganizacionAndIdSolicitud(
                request.getIdOrganizacion(), request.getIdSolicitud());
            if (solicitudOpt.isEmpty() || !solicitudOpt.get().isVigente()) {
                responseObserver.onNext(BajaSolicitudResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Solicitud no encontrada o ya dada de baja")
                    .build());
                responseObserver.onCompleted();
                return;
            }
            // Marcar como no vigente
            SolicitudDonacion solicitud = solicitudOpt.get();
            solicitud.setVigente(false);
            solicitudDonacionRepository.save(solicitud);
            // Registrar baja
            BajaSolicitud baja = new BajaSolicitud();
            baja.setIdOrganizacion(request.getIdOrganizacion());
            baja.setIdSolicitud(request.getIdSolicitud());
            bajaSolicitudRepository.save(baja);
            // Producir mensaje a Kafka
            BajaSolicitudMessage message = new BajaSolicitudMessage();
            message.setIdOrganizacion(request.getIdOrganizacion());
            message.setIdSolicitud(request.getIdSolicitud());
            kafkaProducerService.sendBajaSolicitud(message);
            // Respuesta gRPC
            responseObserver.onNext(BajaSolicitudResponse.newBuilder()
                .setStatus("SUCCESS")
                .setMessage("Solicitud dada de baja exitosamente")
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Error: " + e.getMessage()).asRuntimeException());
        }
    }


    @Override
    public void listarSolicitudes(ListarSolicitudesRequest request, StreamObserver<ListarSolicitudesResponse> responseObserver) {
        try {
            if (!tokenValidator.isTokenValid(request.getToken())) {
                sendErrorResponse(responseObserver, "Token inválido o expirado");
                return;
            }
            List<SolicitudDonacion> solicitudes = solicitudDonacionRepository.findByVigenteTrue();
            ListarSolicitudesResponse.Builder responseBuilder = ListarSolicitudesResponse.newBuilder();
            for (SolicitudDonacion solicitud : solicitudes) {
                SolicitudDonacionRequest.Builder solicitudBuilder = SolicitudDonacionRequest.newBuilder()
                    .setIdOrganizacion(solicitud.getIdOrganizacion())
                    .setIdSolicitud(solicitud.getIdSolicitud());
                for (ItemDonacion item : solicitud.getItems()) {
                    solicitudBuilder.addItems(RpcDonaciones.grpc.DonacionServiceProto.ItemDonacionP.newBuilder()
                        .setCategoria(item.getCategoria())
                        .setDescripcion(item.getDescripcion())
                        .build());
                }
                responseBuilder.addSolicitudes(solicitudBuilder.build());
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            sendErrorResponse(responseObserver, "Error al listar solicitudes: " + e.getMessage());
        }
    }


    @Override
    public void ofrecerDonacion(OfertaDonacionRequest request, StreamObserver<OfertaDonacionResponse> responseObserver) {
        try {
            // Validaciones
            if (request.getIdOrganizacion().isEmpty()) {
                responseObserver.onNext(OfertaDonacionResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("El ID de organización es obligatorio")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            if (request.getItemsList().isEmpty()) {
                responseObserver.onNext(OfertaDonacionResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Debe haber al menos una donación ofrecida")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            if (request.getIdOferta().isEmpty()) {
                responseObserver.onNext(OfertaDonacionResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("El ID de oferta es obligatorio")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            // Validar unicidad del idOferta
            Optional<OfertaSolicitud> existingOferta = ofertaSolicitudRepository
                .findByIdOrganizacionAndIdOferta(request.getIdOrganizacion(), request.getIdOferta());
            if (existingOferta.isPresent()) {
                responseObserver.onNext(OfertaDonacionResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("El ID de oferta ya existe para esta organización")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            // Usar el idOferta del request
            String idOferta = request.getIdOferta();

            // Construir el mensaje para Kafka
            OfertaDonacionMessage message = new OfertaDonacionMessage();
            message.setIdOferta(idOferta);
            message.setIdOrganizacion(request.getIdOrganizacion());
            message.setDonaciones(request.getItemsList().stream()
                .map(item -> new OfertaDonacionMessage.ItemDonacionO(
                    item.getCategoria(),
                    item.getDescripcion(),
                    item.getCantidad()
                ))
                .collect(Collectors.toList()));

            // Enviar el mensaje al topic de Kafka
            kafkaProducerService.sendOfertaDonacion(message);

            System.out.println("Oferta enviada a Kafka: " + idOferta);

            // Respuesta gRPC
            responseObserver.onNext(OfertaDonacionResponse.newBuilder()
                .setStatus("SUCCESS")
                .setMessage("Oferta publicada correctamente en Kafka")
                .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            System.out.println("Error al procesar la oferta: " + e.getMessage());
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Error al procesar la oferta: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }



    @Override
    public void listarOfertas(ListarOfertasRequest request, StreamObserver<ListarOfertasResponse> responseObserver) {
        try {
            if (!tokenValidator.isTokenValid(request.getToken())) {
                responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Token inválido").asRuntimeException());
                return;
            }

            // Consultar las ofertas guardadas en la DB
            List<OfertaSolicitud> ofertasDB = ofertaSolicitudRepository.findAll(); // tu repositorio de ofertas

            List<Oferta> ofertasGrpc = ofertasDB.stream().map(oferta -> {
                List<ItemOferta> itemsGrpc = oferta.getItemsOfertas().stream()
                    .map(item -> ItemOferta.newBuilder()
                        .setCategoria(item.getCategoria())
                        .setDescripcion(item.getDescripcion())
                        .setCantidad(item.getCantidad())
                        .build())
                    .collect(Collectors.toList());

                return Oferta.newBuilder()
                    .setIdOferta(oferta.getIdOferta())
                    .setIdOrganizacion(oferta.getIdOrganizacion())
                    .addAllItems(itemsGrpc)
                    .build();
            }).collect(Collectors.toList());

            ListarOfertasResponse response = ListarOfertasResponse.newBuilder()
                    .addAllOfertas(ofertasGrpc)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Error al listar ofertas: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void transferirDonacion(TransferirDonacionRequest request, StreamObserver<TransferirDonacionResponse> responseObserver) {
        try {
            // Validaciones
            if (request.getIdOrganizacionSolicitante().isEmpty() || request.getIdSolicitud().isEmpty()) {
                responseObserver.onNext(TransferirDonacionResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("ID de organización y solicitud son obligatorios")
                    .build());
                responseObserver.onCompleted();
                return;
            }
            if (request.getItemsList().isEmpty()) {
                responseObserver.onNext(TransferirDonacionResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Debe haber al menos un ítem")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            // Construir mensaje Kafka
            List<TransferenciaDonacionMessage.ItemTransferencia> items = request.getItemsList().stream()
                .map(item -> new TransferenciaDonacionMessage.ItemTransferencia(
                    item.getCategoria(),
                    item.getDescripcion(),
                    item.getCantidad()))
                .collect(Collectors.toList());

            TransferenciaDonacionMessage message = new TransferenciaDonacionMessage();
            message.setIdSolicitud(request.getIdSolicitud());
            message.setIdOrganizacionDonante(obtenerIdOrganizacionLocal());
            message.setDonaciones(items);

            // Enviar mensaje Kafka
            kafkaProducerService.sendTransferenciaDonacion(message);
            System.out.println("Mensaje de transferencia enviado a Kafka: " + request.getIdSolicitud());

            // Responder inmediatamente al cliente gRPC
            responseObserver.onNext(TransferirDonacionResponse.newBuilder()
                .setStatus("SUCCESS")
                .setMessage("Transferencia enviada a Kafka correctamente")
                .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            System.err.println("Error al transferir donación: " + e.getMessage());
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error al procesar transferencia: " + e.getMessage())
                .asRuntimeException());
        }
    }



    @Override
    public void listarSolicitudesExternas(RpcDonaciones.grpc.DonacionServiceProto.ListarSolicitudesRequest request,
            StreamObserver<RpcDonaciones.grpc.DonacionServiceProto.ListarSolicitudesResponse> responseObserver) {
        try {
            if (!tokenValidator.isTokenValid(request.getToken())) {
                sendErrorResponse(responseObserver, "Token invalido o expirado");
                return;
            }

            List<SolicitudDonacion> solicitudes = solicitudDonacionRepository.findByVigenteTrue();
            RpcDonaciones.grpc.DonacionServiceProto.ListarSolicitudesResponse.Builder responseBuilder = RpcDonaciones.grpc.DonacionServiceProto.ListarSolicitudesResponse
                    .newBuilder();

            for (SolicitudDonacion solicitud : solicitudes) {
                if (!solicitud.getIdOrganizacion().equals(obtenerIdOrganizacionLocal())) {
                    RpcDonaciones.grpc.DonacionServiceProto.SolicitudDonacionRequest.Builder solicitudBuilder = RpcDonaciones.grpc.DonacionServiceProto.SolicitudDonacionRequest
                            .newBuilder()
                            .setIdOrganizacion(solicitud.getIdOrganizacion())
                            .setIdSolicitud(solicitud.getIdSolicitud());

                    for (ItemDonacion item : solicitud.getItems()) {
                        solicitudBuilder.addItems(RpcDonaciones.grpc.DonacionServiceProto.ItemDonacionP.newBuilder()
                                .setCategoria(item.getCategoria())
                                .setDescripcion(item.getDescripcion())
                                .build());
                    }
                    responseBuilder.addSolicitudes(solicitudBuilder.build());
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            sendErrorResponse(responseObserver, "Error al listar solicitudes externas: " + e.getMessage());
        }
    }

    private String obtenerIdOrganizacionLocal() {
        return "org-local-id";
    }

    @Override
    public void listarDonacionesParaExcel(ListarDonacionesParaExcelRequest request,
                                        StreamObserver<ListarDonacionesParaExcelResponse> responseObserver) {
        try {
            String token = request.getToken();
            if (!tokenValidator.validarToken(token, responseObserver, "UsuarioResponse")) {
                return;
            }

            List<Donacion> donaciones = donacionRepository.findAll();

            ListarDonacionesParaExcelResponse.Builder response = ListarDonacionesParaExcelResponse.newBuilder()
                    .setStatus("SUCCESS");

            for (Donacion d : donaciones) {
                String fechaEliminacion = "";
                if (d.isEliminado()) {
                    fechaEliminacion = d.getAuditorias().stream()
                            .filter(a -> a.getTipoAccion() == TipoAccion.ELIMINACION)
                            .map(a -> a.getFecha().toString())
                            .findFirst()
                            .orElse("");
                }

                String usuarioModificacion = d.getAuditorias().stream()
                        .filter(a -> a.getTipoAccion() == TipoAccion.MODIFICACION)
                        .map(Auditoria::getUsuario)
                        .reduce((first, second) -> second)
                        .orElse("");

                DonacionParaExcel item = DonacionParaExcel.newBuilder()
                        .setId(d.getId())
                        .setCategoria(d.getCategoria().name())
                        .setDescripcion(d.getDescripcion())
                        .setCantidad(d.getCantidad())
                        .setEliminado(d.isEliminado())
                        .setFechaAlta(d.getFechaAlta().toString())
                        .setUsuarioAlta(d.getUsuarioAlta())
                        .setUsuarioModificacion(usuarioModificacion)
                        .build();

                response.addDonaciones(item);
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error al listar donaciones para Excel: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}


