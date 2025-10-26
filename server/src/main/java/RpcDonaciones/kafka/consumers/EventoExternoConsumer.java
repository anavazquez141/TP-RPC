package RpcDonaciones.kafka.consumers;

import RpcDonaciones.entities.EventoExternoEntity;
import RpcDonaciones.kafka.messages.EventoMessage;
import RpcDonaciones.repositories.IEventoExterno;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventoExternoConsumer {

    private final IEventoExterno eventoExternoRepository;

    // ID de la organización local 
    private static final String ID_ORGANIZACION_LOCAL = "ORG001";

    @KafkaListener(topics = "eventos-solidarios", groupId = "donaciones-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumirEvento(EventoMessage message) {
        try {
            log.info("Evento recibido desde Kafka: {}", message);

            // Descartar eventos propios
            if (message.getIdOrganizacion().equals(ID_ORGANIZACION_LOCAL)) {
                log.info("➡️ Evento descartado: pertenece a la organización local ({})", ID_ORGANIZACION_LOCAL);
                return;
            }

            // Evitar duplicados
            boolean existe = eventoExternoRepository.existsByIdOrganizacionAndIdEvento(
                    message.getIdOrganizacion(),
                    message.getIdEvento()
            );

            if (existe) {
                log.info("⚠️ Evento externo ya registrado (ID: {}, Org: {})",
                        message.getIdEvento(), message.getIdOrganizacion());
                return;
            }

            // Crear entidad EventoExterno
            EventoExternoEntity evento = new EventoExternoEntity();
            evento.setIdOrganizacion(message.getIdOrganizacion());
            evento.setIdEvento(message.getIdEvento());
            evento.setNombreEvento(message.getNombreEvento());
            evento.setDescripcion(message.getDescripcion());
            evento.setFechaHora(message.getFechaHora());
            evento.setVigente(true);

            // Guardar en la base de datos
            eventoExternoRepository.save(evento);
            log.info("✅ Evento externo guardado correctamente: {}", evento.getNombreEvento());

        } catch (Exception e) {
            log.error("Error procesando evento solidario: {}", e.getMessage(), e);
        }
    }
}
