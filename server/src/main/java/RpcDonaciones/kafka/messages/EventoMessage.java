package RpcDonaciones.kafka.messages;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventoMessage {

    private String idOrganizacion;
    private String idEvento;
    private String nombreEvento;
    private String descripcion;
    private LocalDateTime fechaHora;
    private boolean vigente;

    public EventoMessage() {}

    public EventoMessage(String idOrganizacion, String idEvento, String nombreEvento,
                         String descripcion, LocalDateTime fechaHora, boolean vigente) {
        this.idOrganizacion = idOrganizacion;
        this.idEvento = idEvento;
        this.nombreEvento = nombreEvento;
        this.descripcion = descripcion;
        this.fechaHora = fechaHora;
        this.vigente = vigente;
    }
}
