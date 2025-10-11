package RpcDonaciones.kafka.messages;

import lombok.Data;
import java.util.List;

@Data
public class SolicitudDonacionMessage {
    private String idOrganizacion;
    private String idSolicitud;
    private List<ItemDonacion> donaciones;

    @Data
    public static class ItemDonacion {
        private String categoria; // e.g., ALIMENTOS
        private String descripcion; // e.g., Puré de tomates
        public ItemDonacion() {}
        public ItemDonacion(String categoria, String descripcion) {
            this.categoria = categoria;
            this.descripcion = descripcion;
        }
    }
}
