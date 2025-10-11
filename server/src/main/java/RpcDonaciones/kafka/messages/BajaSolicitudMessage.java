package RpcDonaciones.kafka.messages;

import lombok.Data;

@Data
public class BajaSolicitudMessage {
    private String idOrganizacion;
    private String idSolicitud;
}