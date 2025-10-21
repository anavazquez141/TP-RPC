package RpcDonaciones.kafka.producers;

import RpcDonaciones.kafka.messages.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSolicitudDonacion(SolicitudDonacionMessage message) {
        kafkaTemplate.send("solicitud-donaciones", message.getIdSolicitud(), message);
    }

    public void sendBajaSolicitud(BajaSolicitudMessage message) {
        kafkaTemplate.send("baja-solicitud-donaciones", message.getIdSolicitud(), message);
    }

    public void sendOfertaDonacion(OfertaDonacionMessage message) {
    kafkaTemplate.send("oferta-donaciones", message.getIdOferta(), message);
    }
    /*
     public void sendAdhesionEvento(String idOrganizador, AdhesionEventoMessage message) {
        String topicName = "adhesion-evento-" + idOrganizador;
        kafkaTemplate.send(topicName, message.getIdVoluntario(), message);
    }*/

    public void sendBajaEvento(BajaEventoMessage message) {
        kafkaTemplate.send("baja-evento-solidario", message.getEventoId(), message);
    }

    public void sendTransferenciaDonacion(String idOrganizacionSolicitante, TransferenciaDonacionMessage message) {
    String topicName = "transferencia-donaciones-" + idOrganizacionSolicitante;
    kafkaTemplate.send(topicName, message.getIdSolicitud(), message);
    logger.info("Mensaje de transferencia enviado al topic: ", topicName);
    }
}
