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
}