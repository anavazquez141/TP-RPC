package RpcDonaciones.kafka.consumers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import RpcDonaciones.entities.SolicitudDonacion;
import RpcDonaciones.entities.ItemDonacion;
import RpcDonaciones.repositories.ISolicitudDonacion;
import RpcDonaciones.repositories.IBajaSolicitud;
import RpcDonaciones.kafka.messages.SolicitudDonacionMessage;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SolicitudDonacionConsumer {

    @Autowired
    private ISolicitudDonacion solicitudDonacionRepository;

    @Autowired
    private IBajaSolicitud bajaSolicitudRepository;

    @KafkaListener(topics = "solicitud-donaciones", groupId = "donaciones-group")
    public void consumeSolicitudDonacion(SolicitudDonacionMessage message) {
        try {
            // Verificar si la solicitud ya fue dada de baja
            Optional<SolicitudDonacion> existingSolicitud = solicitudDonacionRepository.findByIdOrganizacionAndIdSolicitud(
                message.getIdOrganizacion(), message.getIdSolicitud());
            if (existingSolicitud.isPresent() && !existingSolicitud.get().isVigente()) {
                System.out.println("Solicitud ya no vigente: " + message.getIdSolicitud());
                return;
            }
            if (bajaSolicitudRepository.existsByIdOrganizacionAndIdSolicitud(
                message.getIdOrganizacion(), message.getIdSolicitud())) {
                System.out.println("Solicitud dada de baja: " + message.getIdSolicitud());
                return;
            }
            // Guardar la solicitud
            SolicitudDonacion solicitud = new SolicitudDonacion();
            solicitud.setIdOrganizacion(message.getIdOrganizacion());
            solicitud.setIdSolicitud(message.getIdSolicitud());
            solicitud.setVigente(true);
            solicitud.setItems(message.getDonaciones().stream()
                .map(item -> new ItemDonacion(item.getCategoria(), item.getDescripcion()))
                .collect(Collectors.toList()));
            solicitudDonacionRepository.save(solicitud);
            System.out.println("Solicitud guardada: " + message.getIdSolicitud());
        } catch (Exception e) {
            System.err.println("Error al procesar mensaje de solicitud-donaciones: " + e.getMessage());
        }
    }
}