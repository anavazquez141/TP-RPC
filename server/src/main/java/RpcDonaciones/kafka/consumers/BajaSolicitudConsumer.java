package RpcDonaciones.kafka.consumers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import RpcDonaciones.entities.SolicitudDonacion;
import RpcDonaciones.entities.BajaSolicitud;
import RpcDonaciones.repositories.ISolicitudDonacion;
import RpcDonaciones.repositories.IBajaSolicitud;
import RpcDonaciones.kafka.messages.BajaSolicitudMessage;

import java.util.Optional;

@Component
public class BajaSolicitudConsumer {
    private static final Logger logger = LoggerFactory.getLogger(BajaSolicitudConsumer.class);
    
    @Autowired
    private ISolicitudDonacion solicitudDonacionRepository;

    @Autowired
    private IBajaSolicitud bajaSolicitudRepository;

    @KafkaListener(topics = "baja-solicitud-donaciones", groupId = "donaciones-group")
    public void consumeBajaSolicitud(BajaSolicitudMessage message) {
        try {
            Optional<SolicitudDonacion> solicitudOpt = solicitudDonacionRepository.findByIdOrganizacionAndIdSolicitud(
                message.getIdOrganizacion(), message.getIdSolicitud());

            if (solicitudOpt.isPresent()) {
                SolicitudDonacion solicitud = solicitudOpt.get();
                solicitud.setVigente(false);
                solicitudDonacionRepository.save(solicitud);

                BajaSolicitud baja = new BajaSolicitud();
                baja.setIdOrganizacion(message.getIdOrganizacion());
                baja.setIdSolicitud(message.getIdSolicitud());
                bajaSolicitudRepository.save(baja);

                logger.info("✅ Baja procesada correctamente: idOrganizacion={}, idSolicitud={}",
                        message.getIdOrganizacion(), message.getIdSolicitud());
            } else {
                logger.warn("⚠️ No se encontró solicitud para baja: idOrganizacion={}, idSolicitud={}",
                        message.getIdOrganizacion(), message.getIdSolicitud());
            }

        } catch (Exception e) {
            logger.error("❌ Error al procesar baja de solicitud: {}", e.getMessage(), e);
        }
}


}