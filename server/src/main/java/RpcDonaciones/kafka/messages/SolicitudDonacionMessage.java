package RpcDonaciones.kafka.messages;

import lombok.Data;
import java.util.List;

@Data
public class SolicitudDonacionMessage {
    private String idOrganizacion;
    private String idSolicitud;
    private List<ItemDonacionM> donaciones;

    @Data
    public static class ItemDonacionM {
        private String categoria; // e.g., ALIMENTOS
        private String descripcion; // e.g., Puré de tomates
        public ItemDonacionM() {}
        public ItemDonacionM(String categoria, String descripcion) {
            this.categoria = categoria;
            this.descripcion = descripcion;
        }
    }
}
