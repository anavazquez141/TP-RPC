package RpcDonaciones.services;

import RpcDonaciones.grpc.EventoSolidarioServiceProto.CreateEventoRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.CreateEventoResponse;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.DeleteEventoRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.DeleteEventoResponse;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.Evento;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.EventoSinUsuarios;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.GetEventoRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.GetEventoResponse;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.ListEventosRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.ListEventosResponse;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.UpdateEventoRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.UpdateEventoResponse;
import RpcDonaciones.grpc.EventosServiceGrpc;
import RpcDonaciones.grpc.UsuarioServiceProto;
import RpcDonaciones.grpc.UsuarioServiceProto.UsuarioSinClave;
import RpcDonaciones.services.EventosServiceImpl;
import RpcDonaciones.repositories.IEventoSolidario;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.entities.EventoSolidario;
import RpcDonaciones.entities.Usuario;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

@Service
public class EventosServiceImpl extends EventosServiceGrpc.EventosServiceImplBase {

    @Autowired
    private IEventoSolidario eventosRepository;

    @Autowired
    private IUsuario usuarioRepository;

    private final TokenValidator tokenValidator;
    private final SecretKey key;

    public EventosServiceImpl(@Value("${jwt.secret}") String base64Key, TokenValidator tokenValidator) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key));
        this.tokenValidator = tokenValidator;
    }

    @Override
    public void createEvento(CreateEventoRequest request, StreamObserver<CreateEventoResponse> responseObserver) {
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
            if (!roles.contains("ROLE_PRESIDENTE") && !roles.contains("ROLE_COORDINADOR")) {
                responseObserver.onNext(CreateEventoResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("No tienes permiso para crear eventos")
                    .build());
                responseObserver.onCompleted();
                return;
            }

        EventoSolidario evento = new EventoSolidario();
        evento.setNombreEvento(request.getNombreEvento());
        evento.setDescripcion(request.getDescripcion());
        evento.setFechaHora(LocalDateTime.parse(request.getFechaHora(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        List<Usuario> usuarios = request.getUsuarioIdsList().stream()
                .map(id -> usuarioRepository.findById(id).orElseThrow())
                .collect(Collectors.toList());
        evento.setUsuarios(usuarios);

        EventoSolidario saved = eventosRepository.save(evento);
        CreateEventoResponse response = CreateEventoResponse.newBuilder()
                .setEvento(toProto(saved))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error al registrar evento: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void getEvento(GetEventoRequest request, StreamObserver<GetEventoResponse> responseObserver) {
        try {
            String token = request.getToken();
            if (!tokenValidator.validarToken(token, responseObserver, "GetEventoResponse")) {
                return;
            }

            Optional<EventoSolidario> optional = eventosRepository.findById(request.getIdEvento());
            if (optional.isPresent()) {
                GetEventoResponse response = GetEventoResponse.newBuilder()
                        .setEvento(toEventoSinUsuariosProto(optional.get()))
                        .setStatus("OK")
                        .setMessage("Evento encontrado")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Evento no encontrado")
                        .asRuntimeException());
            }
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error al obtener evento: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateEvento(UpdateEventoRequest request, StreamObserver<UpdateEventoResponse> responseObserver) {
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
            if (!roles.contains("ROLE_PRESIDENTE") && !roles.contains("ROLE_COORDINADOR")) {
                responseObserver.onNext(UpdateEventoResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("No tienes permiso para modificar eventos")
                    .build());
                responseObserver.onCompleted();
                return;
            }

        Optional<EventoSolidario> optional = eventosRepository.findById(request.getIdEvento());
        if (optional.isPresent()) {
            EventoSolidario evento = optional.get();
            if (!request.getNombreEvento().isEmpty()) evento.setNombreEvento(request.getNombreEvento());
            if (!request.getDescripcion().isEmpty()) evento.setDescripcion(request.getDescripcion());
            if (!request.getFechaHora().isEmpty()) evento.setFechaHora(LocalDateTime.parse(request.getFechaHora(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            if (!request.getUsuarioIdsList().isEmpty()) {
                List<Usuario> usuarios = request.getUsuarioIdsList().stream()
                        .map(id -> usuarioRepository.findById(id).orElseThrow())
                        .collect(Collectors.toList());
                evento.setUsuarios(usuarios);
            }

            EventoSolidario updated = eventosRepository.save(evento);
            UpdateEventoResponse response = UpdateEventoResponse.newBuilder()
                    .setEvento(toProto(updated))
                    .build();
            responseObserver.onNext(response);
        } else {
            responseObserver.onError(new RuntimeException("Evento not found"));
        }
        responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error al modificar evento: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void deleteEvento(DeleteEventoRequest request, StreamObserver<DeleteEventoResponse> responseObserver) {
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
            if (!roles.contains("ROLE_PRESIDENTE") && !roles.contains("ROLE_COORDINADOR")) {
                responseObserver.onNext(DeleteEventoResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("No tienes permiso para eliminar eventos")
                    .build());
                responseObserver.onCompleted();
                return;
            }
        Optional<EventoSolidario> optional = eventosRepository.findById(request.getIdEvento());
            if (optional.isPresent()) {
                EventoSolidario evento = optional.get();
                // Verificar si el evento ya pasó
                LocalDateTime fechaEvento = evento.getFechaHora();
                LocalDateTime ahora = LocalDateTime.now();
                if (fechaEvento.isBefore(ahora)) {
                    responseObserver.onNext(DeleteEventoResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("No se pueden eliminar eventos pasados")
                            .build());
                    responseObserver.onCompleted();
                    return;
                }

            eventosRepository.deleteById(request.getIdEvento());
            DeleteEventoResponse response = DeleteEventoResponse.newBuilder()
                    .setSuccess(true)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            } else {
                    responseObserver.onError(Status.NOT_FOUND
                            .withDescription("Evento no encontrado")
                            .asRuntimeException());
                }
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error al eliminar evento: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void listEventos(ListEventosRequest request, StreamObserver<ListEventosResponse> responseObserver) {
        try {
            String token = request.getToken();
            if (!tokenValidator.validarToken(token, responseObserver, "ListEventosResponse")) {
                return;
            }

            List<EventoSolidario> eventos = eventosRepository.findAll();
            ListEventosResponse response = ListEventosResponse.newBuilder()
                    .addAllEventos(eventos.stream().map(this::toEventoSinUsuariosProto).collect(Collectors.toList()))
                    .setStatus("OK")
                    .setMessage("Eventos listados exitosamente")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error al listar eventos: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private Evento toProto(EventoSolidario entity) {
        Evento.Builder builder = Evento.newBuilder()
                .setIdEvento(entity.getIdEvento())
                .setNombreEvento(entity.getNombreEvento())
                .setDescripcion(entity.getDescripcion())
                .setFechaHora(entity.getFechaHora().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Usar el método auxiliar para evitar complejidad en stream
        List<UsuarioSinClave> usuariosProto = entity.getUsuarios().stream()
                .map(this::toUsuarioSinClave)
                .collect(Collectors.toList());
        builder.addAllUsuarios(usuariosProto);

        return builder.build();
    }

    private UsuarioSinClave toUsuarioSinClave(Usuario u) {
        // Aquí puedes agregar validaciones si es necesario
        return UsuarioSinClave.newBuilder()
                .setId(u.getId())
                .setNombreUsuario(u.getNombreUsuario())
                .setNombre(u.getNombre())
                .setApellido(u.getApellido())
                .setTelefono(u.getTelefono())
                .setEmail(u.getEmail())
                .setRol(RoleMapper.mapTipoDeRolToProtoRole(u.getRolUsuario().iterator().next().getType()))  // Ver nota abajo sobre enums
                .setActivo(u.isEstado())
                .build();
    }

    private EventoSinUsuarios toEventoSinUsuariosProto(EventoSolidario entity) {
        return EventoSinUsuarios.newBuilder()
                .setIdEvento(entity.getIdEvento())
                .setNombreEvento(entity.getNombreEvento())
                .setDescripcion(entity.getDescripcion())
                .setFechaHora(entity.getFechaHora().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}