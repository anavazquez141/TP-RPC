package RpcDonaciones.kafka.consumers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import RpcDonaciones.kafka.messages.TransferenciaDonacionMessage;
import RpcDonaciones.services.InventarioService;

@Component
public class TransferenciaDonacionConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(TransferenciaDonacionConsumer.class);
    
    @Autowired
    private InventarioService inventarioService;

    @KafkaListener(topicPattern = "transferencia-donaciones-.*", groupId = "transferencias-group")
    public void consumeTransferenciaDonacion(TransferenciaDonacionMessage message) {
        try {
            logger.info("Procesando transferencia de donacion para solicitud: ", message.getIdSolicitud());
            logger.info("Organizacion donante: ", message.getIdOrganizacionDonante());
            
            for (TransferenciaDonacionMessage.ItemTransferencia item : message.getDonaciones()) {
                inventarioService.agregarAlInventario(
                    item.getCategoria(),
                    item.getDescripcion(),
                    item.getCantidad()
                );
                logger.info("Agregado al inventario: - Cantidad: ", 
                    item.getCategoria(), item.getDescripcion(), item.getCantidad());
            }
            
            logger.info("Transferencia procesada correctamente. Solicitud: ", message.getIdSolicitud());
                
        } catch (Exception e) {
            logger.error("Error al procesar transferencia de donacion: ", e.getMessage(), e);
        }
    }
}
