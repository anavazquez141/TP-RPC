package RpcDonaciones.kafka.messages;

import lombok.Data;
import java.util.List;

@Data
public class OfertaDonacionMessage {

    private String idOferta;
    private String idOrganizacion;
    private List<ItemDonacionO> donaciones;

    @Data
    public static class ItemDonacionO {
        private String categoria;    // e.g., ALIMENTOS
        private String descripcion;  // e.g., Puré de tomates
        private long cantidad;        // e.g., 2kg o unidades

        // Constructor vacío
        public ItemDonacionO() {}

        // Constructor con parámetros
        public ItemDonacionO(String categoria, String descripcion, long cantidad) {
            this.categoria = categoria;
            this.descripcion = descripcion;
            this.cantidad = cantidad;
        }
    }
}