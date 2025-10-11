package RpcDonaciones.kafka.consumers;

import RpcDonaciones.kafka.messages.SolicitudDonacionMessage;
import RpcDonaciones.entities.SolicitudDonacion;
import RpcDonaciones.entities.ItemDonacion;
import RpcDonaciones.repositories.ISolicitudDonacion;
import RpcDonaciones.repositories.IBajaSolicitud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class KafkaConsumerService {

    @Autowired
    private ISolicitudDonacion solicitudDonacionRepository;

    @Autowired
    private IBajaSolicitud bajaSolicitudRepository;

    private static final String MI_ORG_ID = "MI_ORG_ID"; // Reemplaza con el ID de tu ONG

    @KafkaListener(topics = "solicitud-donaciones", groupId = "ong-group")
    public void consumeSolicitudDonacion(SolicitudDonacionMessage message) {
        // Ignorar mensajes propios
        if (message.getIdOrganizacion().equals(MI_ORG_ID)) {
            return;
        }
        // Verificar si la solicitud no está dada de baja
        if (!bajaSolicitudRepository.existsByIdOrganizacionAndIdSolicitud(message.getIdOrganizacion(), message.getIdSolicitud())) {
            // Mapear mensaje a entidad
            SolicitudDonacion solicitud = new SolicitudDonacion();
            solicitud.setIdOrganizacion(message.getIdOrganizacion());
            solicitud.setIdSolicitud(message.getIdSolicitud());
            solicitud.setItems(message.getDonaciones().stream()
                .map(item -> new ItemDonacion(item.getCategoria(), item.getDescripcion()))
                .collect(Collectors.toList()));
            solicitud.setVigente(true);
            // Guardar en BD
            solicitudDonacionRepository.save(solicitud);
        }
    }
}