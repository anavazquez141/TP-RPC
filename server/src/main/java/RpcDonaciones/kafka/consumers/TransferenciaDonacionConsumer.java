package RpcDonaciones.kafka.consumers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import RpcDonaciones.entities.TransferenciaDonacion;
import RpcDonaciones.entities.ItemTransferencia;
import RpcDonaciones.repositories.ITransferenciaDonacion;
import RpcDonaciones.services.InventarioService;
import RpcDonaciones.kafka.messages.TransferenciaDonacionMessage;

import java.util.stream.Collectors;

@Component
public class TransferenciaDonacionConsumer {

    @Autowired
    private ITransferenciaDonacion transferenciaDonacionRepository;

    @Autowired
    private InventarioService inventarioService;

    @KafkaListener(topics = "transferencia-donaciones", groupId = "donaciones-group")
    public void consumeTransferenciaDonacion(TransferenciaDonacionMessage message) {
        try {
            System.out.println("Procesando transferencia de donacion para solicitud: " + message.getIdSolicitud());
            System.out.println("Organizacion donante: " + message.getIdOrganizacionDonante());

            // Guardar la transferencia en la DB
            TransferenciaDonacion transferencia = new TransferenciaDonacion();
            transferencia.setIdSolicitud(message.getIdSolicitud());
            transferencia.setIdOrganizacionSolicitante(message.getIdOrganizacionDonante());
            transferencia.setItems(message.getDonaciones().stream()
                    .map(item -> new ItemTransferencia(
                            item.getCategoria(),
                            item.getDescripcion(),
                            item.getCantidad()))
                    .collect(Collectors.toList()));

            transferenciaDonacionRepository.save(transferencia);

            // Actualizar inventario
            for (ItemTransferencia item : transferencia.getItems()) {
                inventarioService.agregarAlInventario(
                        item.getCategoria(),
                        item.getDescripcion(),
                        item.getCantidad()
                );
                System.out.println("Agregado al inventario: " +
                        item.getCategoria() + ", " + item.getDescripcion() + ", " + item.getCantidad());
            }

            System.out.println("Transferencia procesada correctamente. Solicitud: " + message.getIdSolicitud());

        } catch (Exception e) {
            System.err.println("Error al procesar transferencia de donacion: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
