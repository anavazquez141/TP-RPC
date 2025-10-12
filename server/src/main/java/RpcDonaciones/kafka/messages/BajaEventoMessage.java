package RpcDonaciones.kafka.messages;

import lombok.Value;

/**
 * Representa el mensaje de un evento dado de baja que se publica en el topic.
 */
@Value
public class BajaEventoMessage {

    /**
     * ID de la organización que dio de baja el evento.
     */
    private final String organizacionId;

    /**
     * ID del evento que ha sido dado de baja.
     */
    private final String eventoId;
}