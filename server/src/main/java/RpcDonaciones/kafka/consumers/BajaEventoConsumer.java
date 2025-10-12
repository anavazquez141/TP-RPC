package RpcDonaciones.kafka.consumers;
import RpcDonaciones.kafka.messages.BajaEventoMessage;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class BajaEventoConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(BajaEventoConsumer.class);

    /**
     * Escucha los mensajes en el topic "/baja-evento-solidario".
     *
     * @param message El objeto BajaEventoMessage deserializado.
     * @param record El ConsumerRecord original (opcional, útil para metadata).
     */
    @KafkaListener(
        topics = "baja-evento-solidario",           // Nombre del topic
        groupId = "baja-eventos-group"              // ID del grupo de consumidores
    )
    public void consumeBajaEvento(BajaEventoMessage message, ConsumerRecord<String, BajaEventoMessage> record) {

        LOG.info("🚨 Recibido mensaje de baja de evento en el topic: {}", record.topic());
        LOG.info("  - Partition: {}, Offset: {}", record.partition(), record.offset());
        LOG.info("  - Organizacion ID: {}", message.getOrganizacionId());
        LOG.info("  - Evento ID: {}", message.getEventoId());

        
        try {

             if (message.getEventoId().isEmpty()) {
                 throw new IllegalArgumentException("El ID del evento no puede estar vacío.");
             }
             
             LOG.info("✅ Evento {} de la organización {} procesado y dado de baja con éxito.",
                     message.getEventoId(), message.getOrganizacionId());

        } catch (Exception e) {
            LOG.error("❌ Error al procesar la baja del evento {}: {}", message.getEventoId(), e.getMessage());

        }
    }
}