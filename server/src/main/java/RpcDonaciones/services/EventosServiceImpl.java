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
import RpcDonaciones.grpc.EventoSolidarioServiceProto.PublicarEventoRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.PublicarEventoResponse;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.ListarEventosExternosRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.ListarEventosExternosResponse;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.EventoExterno;

import RpcDonaciones.grpc.EventosServiceGrpc;
import RpcDonaciones.grpc.UsuarioServiceProto;
import RpcDonaciones.grpc.UsuarioServiceProto.UsuarioSinClave;
import RpcDonaciones.services.EventosServiceImpl;
import RpcDonaciones.repositories.IEventoSolidario;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.repositories.IEventoExterno;
import RpcDonaciones.entities.EventoSolidario;
import RpcDonaciones.entities.Usuario;
import RpcDonaciones.entities.EventoExternoEntity;
import RpcDonaciones.kafka.messages.EventoMessage;
import RpcDonaciones.kafka.producers.KafkaProducerService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.Collections;
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
    
    @Autowired
    private IEventoExterno eventoExternoRepository;
    
    @Autowired
    private KafkaProducerService kafkaProducerService;
    private final DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"); 

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
        if (evento.getFechaHora().isBefore(LocalDateTime.now())) {
            responseObserver.onNext(CreateEventoResponse.newBuilder()
                .setStatus("FAILURE")
                .setMessage("La fecha y hora del evento no puede ser en el pasado")
                .build());
            responseObserver.onCompleted();
            return;
            
        }    

        List<Usuario> usuarios = request.getUsuarioIdsList().stream()
                .map(id -> usuarioRepository.findById(id).orElseThrow())
                .collect(Collectors.toList());
        evento.setUsuarios(usuarios);

        EventoSolidario saved = eventosRepository.save(evento);
        CreateEventoResponse response = CreateEventoResponse.newBuilder()
                .setEvento(toProto(saved))
                .setStatus("SUCCESS")
                .setMessage("Evento registrado exitosamente")
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
                        .setEvento(toProto(optional.get()))
                        .setStatus("SUCCESS")
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

            if (!request.getFechaHora().isEmpty()) {
                LocalDateTime fechaHora = LocalDateTime.parse(request.getFechaHora(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                if (fechaHora.isBefore(LocalDateTime.now())) {
                    responseObserver.onNext(UpdateEventoResponse.newBuilder()
                            .setStatus("FAILURE")
                            .setMessage("No puedes cambiar datos de un evento del pasado")
                            .build());
                    responseObserver.onCompleted();
                    return;
                }
                evento.setFechaHora(fechaHora);
            }

            if (!request.getNombreEvento().isEmpty()) {
                evento.setNombreEvento(request.getNombreEvento());
            }
            if (!request.getDescripcion().isEmpty()) {
                evento.setDescripcion(request.getDescripcion());
            }

            // Siempre actualiza la lista de usuarios, incluso si está vacía
            List<Usuario> usuarios = request.getUsuarioIdsList().stream()
                    .map(id -> usuarioRepository.findById(id).orElseThrow())
                    .collect(Collectors.toList());
            evento.setUsuarios(usuarios);

            EventoSolidario updated = eventosRepository.save(evento);
            UpdateEventoResponse response = UpdateEventoResponse.newBuilder()
                    .setEvento(toProto(updated))
                    .setStatus("SUCCESS")
                    .setMessage("Evento actualizado exitosamente")
                    .build();
            responseObserver.onNext(response);
        } else {
            responseObserver.onNext(UpdateEventoResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Evento no encontrado")
                    .build());
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
                    .setStatus("SUCCESS")
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

    @Override
    public void asignarseEvento(UpdateEventoRequest request, StreamObserver<UpdateEventoResponse> responseObserver) {
        try {
            String token = request.getToken();
            System.out.println("AsignarseEvento recibido: idEvento=" + request.getIdEvento() + ", usuarioIds=" + request.getUsuarioIdsList());

            if (!tokenValidator.validarToken(token, responseObserver, "UsuarioResponse")) {
                return;
            }

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseClaimsJws(token)
                    .getPayload();

            List<String> roles = claims.get("roles", List.class);
            if (!roles.contains("ROLE_VOLUNTARIO")) {
                responseObserver.onNext(UpdateEventoResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("No tienes permiso para realizar esta acción")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            if (request.getUsuarioIdsList().isEmpty()) {
                System.out.println("Error: No se proporcionó un ID de usuario");
                responseObserver.onNext(UpdateEventoResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("No se proporcionó un ID de usuario")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            Long userId = request.getUsuarioIdsList().get(0);
            Optional<EventoSolidario> optionalEvento = eventosRepository.findById(request.getIdEvento());
            Optional<Usuario> optionalUsuario = usuarioRepository.findById(userId);
            if (optionalEvento.isPresent() && optionalUsuario.isPresent()) {
                EventoSolidario evento = optionalEvento.get();
                Usuario usuario = optionalUsuario.get();

                if (evento.getUsuarios().contains(usuario)) {
                    responseObserver.onNext(UpdateEventoResponse.newBuilder()
                            .setStatus("FAILURE")
                            .setMessage("El usuario ya está asignado a este evento")
                            .build());
                    responseObserver.onCompleted();
                    return;
                }

                evento.getUsuarios().add(usuario);
                EventoSolidario updated = eventosRepository.save(evento);
                UpdateEventoResponse response = UpdateEventoResponse.newBuilder()
                        .setEvento(toProto(updated))
                        .setStatus("SUCCESS")
                        .setMessage("Usuario asignado al evento exitosamente")
                        .build();
                responseObserver.onNext(response);
            } else {
                responseObserver.onNext(UpdateEventoResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("Evento o usuario no encontrado")
                        .build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error al asignarse al evento: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private Evento toProto(EventoSolidario entity) {
        Evento.Builder builder = Evento.newBuilder()
                .setIdEvento(entity.getIdEvento())
                .setNombreEvento(entity.getNombreEvento())
                .setDescripcion(entity.getDescripcion())
                .setFechaHora(entity.getFechaHora().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        List<UsuarioSinClave> usuariosProto = entity.getUsuarios() != null
                ? entity.getUsuarios().stream()
                    .map(this::toUsuarioSinClave)
                    .collect(Collectors.toList())
                : Collections.emptyList();
        builder.addAllUsuarios(usuariosProto);

        System.out.println("Evento " + entity.getIdEvento() + ": usuarios=" + usuariosProto);
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

    
    @Override
    public void publicarEventoExterno(PublicarEventoRequest request,
                                    StreamObserver<PublicarEventoResponse> responseObserver) {
        try {
            // Validaciones
            if (request.getIdOrganizacion().isEmpty()) {
                responseObserver.onNext(PublicarEventoResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("El ID de organización es obligatorio")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            if (request.getIdEvento().isEmpty()) {
                responseObserver.onNext(PublicarEventoResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("El ID de evento es obligatorio")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            if (request.getNombreEvento().isEmpty()) {
                responseObserver.onNext(PublicarEventoResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("El nombre del evento es obligatorio")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            if (request.getDescripcion().isEmpty()) {
                responseObserver.onNext(PublicarEventoResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("La descripción del evento es obligatoria")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            if (request.getFechaHora().isEmpty()) {
                responseObserver.onNext(PublicarEventoResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("La fecha y hora del evento es obligatoria")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // Validar unicidad del evento en la organización
            Optional<EventoExternoEntity> existingEvento = eventoExternoRepository
                    .findByIdOrganizacionAndIdEvento(request.getIdOrganizacion(), request.getIdEvento());

            if (existingEvento.isPresent()) {
                responseObserver.onNext(PublicarEventoResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("El evento ya existe para esta organización")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // Parse de fecha y hora con formato correcto
            LocalDateTime fechaHora = LocalDateTime.parse(request.getFechaHora(), fechaFormatter);

            // Guardar en DB
            EventoExternoEntity eventoExterno = new EventoExternoEntity();
            eventoExterno.setIdOrganizacion(request.getIdOrganizacion());
            eventoExterno.setIdEvento(request.getIdEvento());
            eventoExterno.setNombreEvento(request.getNombreEvento());
            eventoExterno.setDescripcion(request.getDescripcion());
            eventoExterno.setFechaHora(fechaHora);
            eventoExterno.setVigente(true);

            eventoExternoRepository.save(eventoExterno);

            // Construir mensaje Kafka
            EventoMessage message = new EventoMessage();
            message.setIdOrganizacion(request.getIdOrganizacion());
            message.setIdEvento(request.getIdEvento());
            message.setNombreEvento(request.getNombreEvento());
            message.setDescripcion(request.getDescripcion());
            message.setFechaHora(fechaHora);
            message.setVigente(true);

            kafkaProducerService.sendEvento(message);

            System.out.println("Evento externo publicado en Kafka: " + request.getIdEvento());

            // Respuesta gRPC
            responseObserver.onNext(PublicarEventoResponse.newBuilder()
                    .setStatus("SUCCESS")
                    .setMessage("Evento publicado correctamente")
                    .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            System.out.println("Error al publicar evento externo: " + e.getMessage());
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error al publicar evento externo: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void listarEventosExternos(ListarEventosExternosRequest request,
                                    StreamObserver<ListarEventosExternosResponse> responseObserver) {
        try {
            //  Validación del token
            if (request.getToken().isEmpty()) {
                responseObserver.onNext(ListarEventosExternosResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("El token es obligatorio")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            //  Parse del token para obtener la organización del usuario
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseClaimsJws(request.getToken())
                    .getPayload();
            String idOrgUsuario = claims.get("idOrganizacion", String.class); // <--- ID de la organización del usuario

            //  Obtener todos los eventos externos desde la DB
            List<EventoExternoEntity> eventosExternos = eventoExternoRepository.findAll();

            //  Filtrar eventos: descartar propios y descartar no vigentes
            List<EventoExternoEntity> eventosFiltrados = eventosExternos.stream()
                    .filter(e -> !e.getIdOrganizacion().equals(idOrgUsuario)) // descartar propios
                    .filter(EventoExternoEntity::isVigente) // solo eventos vigentes
                    .collect(Collectors.toList());

            // Convertir cada EventoExterno a EventoExterno del proto
            List<EventoExterno> protoEventos = eventosFiltrados.stream()
                    .map(e -> EventoExterno.newBuilder()
                            .setIdEvento(e.getIdEvento())
                            .setIdOrganizacion(e.getIdOrganizacion())
                            .setNombreEvento(e.getNombreEvento())
                            .setDescripcion(e.getDescripcion())
                            .setFechaHora(e.getFechaHora().toString())
                            .build())
                    .collect(Collectors.toList());

            // 6️⃣ Construir la respuesta gRPC
            ListarEventosExternosResponse response = ListarEventosExternosResponse.newBuilder()
                    .addAllEventos(protoEventos)
                    .setStatus("SUCCESS")
                    .setMessage("Eventos externos obtenidos correctamente")
                    .build();

            // 7️⃣ Enviar la respuesta
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            System.out.println("Error al listar eventos externos: " + e.getMessage());
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error al listar eventos externos: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }


    @Override
    public void listarParticipacionesEventos(ListarParticipacionesRequest request,
                                            StreamObserver<ListarParticipacionesResponse> responseObserver) {
        try {
            String token = request.getToken();
            if (!tokenValidator.validarToken(token, responseObserver, "UsuarioResponse")) {
                return;
            }

            List<EventoSolidario> eventos = eventoRepository.findAll();
            ListarParticipacionesResponse.Builder response = ListarParticipacionesResponse.newBuilder();

            for (EventoSolidario e : eventos) {
                List<Usuario> usuarios = e.getUsuarios();
                if (usuarios == null || usuarios.isEmpty()) continue;

                String fechaStr = e.getFechaHora().toString();

                for (Usuario u : usuarios) {
                    ParticipacionEvento item = ParticipacionEvento.newBuilder()
                        .setEventoId(e.getIdEvento())
                        .setEventoNombre(e.getNombreEvento())
                        .setFechaEvento(fechaStr)
                        .setUsuario(u.getUsername())
                        .build();
                    response.addParticipaciones(item);
                }
            }

            response.setStatus("SUCCESS");
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

}