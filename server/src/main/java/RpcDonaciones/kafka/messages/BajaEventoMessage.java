package RpcDonaciones.kafka.messages;

import lombok.Value;

/**
 * Representa el mensaje de un evento dado de baja que se publica en el topic.
 */
@Value
public class BajaEventoMessage {

    
    private final String organizacionId;
    private final String eventoId;
}