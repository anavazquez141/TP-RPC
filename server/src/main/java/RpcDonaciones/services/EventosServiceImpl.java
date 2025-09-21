package RpcDonaciones.services;

import RpcDonaciones.grpc.EventoSolidarioServiceProto.CreateEventoRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.CreateEventoResponse;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.DeleteEventoRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.DeleteEventoResponse;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.Evento;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.GetEventoRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.GetEventoResponse;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.ListEventosRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.ListEventosResponse;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.UpdateEventoRequest;
import RpcDonaciones.grpc.EventoSolidarioServiceProto.UpdateEventoResponse;
import RpcDonaciones.grpc.EventosServiceGrpc;
import RpcDonaciones.services.EventosServiceImpl;
import RpcDonaciones.repositories.IEventoSolidario;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.entities.EventoSolidario;
import RpcDonaciones.entities.Usuario;
import io.grpc.stub.StreamObserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventosServiceImpl extends EventosServiceGrpc.EventosServiceImplBase {

    @Autowired
    private IEventoSolidario eventosRepository;

    @Autowired
    private IUsuario usuarioRepository;

    @Override
    public void createEvento(CreateEventoRequest request, StreamObserver<CreateEventoResponse> responseObserver) {
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
    }

    @Override
    public void getEvento(GetEventoRequest request, StreamObserver<GetEventoResponse> responseObserver) {
        Optional<EventoSolidario> optional = eventosRepository.findById(request.getIdEvento());
        if (optional.isPresent()) {
            GetEventoResponse response = GetEventoResponse.newBuilder()
                    .setEvento(toProto(optional.get()))
                    .build();
            responseObserver.onNext(response);
        } else {
            responseObserver.onError(new RuntimeException("Evento not found"));
        }
        responseObserver.onCompleted();
    }

    @Override
    public void updateEvento(UpdateEventoRequest request, StreamObserver<UpdateEventoResponse> responseObserver) {
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
    }

    @Override
    public void deleteEvento(DeleteEventoRequest request, StreamObserver<DeleteEventoResponse> responseObserver) {
        eventosRepository.deleteById(request.getIdEvento());
        DeleteEventoResponse response = DeleteEventoResponse.newBuilder()
                .setSuccess(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listEventos(ListEventosRequest request, StreamObserver<ListEventosResponse> responseObserver) {
        List<EventoSolidario> eventos = eventosRepository.findAll();
        ListEventosResponse response = ListEventosResponse.newBuilder()
                .addAllEventos(eventos.stream().map(this::toProto).collect(Collectors.toList()))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private Evento toProto(EventoSolidario entity) {
        Evento.Builder builder = Evento.newBuilder()
                .setIdEvento(entity.getIdEvento())
                .setNombreEvento(entity.getNombreEvento())
                .setDescripcion(entity.getDescripcion())
                .setFechaHora(entity.getFechaHora().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        /*entity.getUsuarios().forEach(u -> builder.addUsuarios(Usuario.builder()
                .setIdUsuario(u.getId())
                .setNombre(u.getNombre())
                .build()));*/
        return builder.build();
    }
}