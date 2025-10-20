package RpcDonaciones.kafka.messages;

import lombok.Data;
import java.util.List;

@Data
public class TransferenciaDonacionMessage {
    private String idSolicitud;
    private String idOrganizacionDonante;
    private List<ItemTransferencia> donaciones;

    @Data
    public static class ItemTransferencia {
        private String categoria;
        private String descripcion;
        private long cantidad;

        public ItemTransferencia() {}

        public ItemTransferencia(String categoria, String descripcion, long cantidad) {
            this.categoria = categoria;
            this.descripcion = descripcion;
            this.cantidad = cantidad;
        }
    }
}