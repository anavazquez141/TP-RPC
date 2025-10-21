package RpcDonaciones.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import RpcDonaciones.kafka.messages.TransferenciaDonacionMessage;
import RpcDonaciones.kafka.producers.KafkaProducerService;

import java.util.List;

@Service
public class TransferenciaService {
    
    @Autowired
    private KafkaProducerService kafkaProducerService;
    
    @Autowired
    private InventarioService inventarioService;

    public void enviarTransferenciaDonacion(
            String idOrganizacionSolicitante,
            String idSolicitud,
            List<TransferenciaDonacionMessage.ItemTransferencia> items) {
        
        try {
            for (TransferenciaDonacionMessage.ItemTransferencia item : items) {
                boolean disponible = inventarioService.verificarYDescontarInventario(
                    item.getCategoria(),
                    item.getDescripcion(),
                    item.getCantidad()
                );
                
                if (!disponible) {
                    throw new RuntimeException("Inventario insuficiente para: " + item.getDescripcion());
                }
            }
            
            TransferenciaDonacionMessage message = new TransferenciaDonacionMessage();
            message.setIdSolicitud(idSolicitud);
            message.setIdOrganizacionDonante(obtenerIdOrganizacionLocal());
            message.setDonaciones(items);
            
            kafkaProducerService.sendTransferenciaDonacion(idOrganizacionSolicitante, message);
            
            System.out.println("Transferencia enviada correctamente a organizacion: " + idOrganizacionSolicitante);
            
        } catch (Exception e) {
            System.err.println("Error al enviar transferencia: " + e.getMessage());
            throw e;
        }
    }
    
    private String obtenerIdOrganizacionLocal() {
        return "org-local-id";
    }
}