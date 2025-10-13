package RpcDonaciones.kafka.consumers;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import RpcDonaciones.entities.OfertaSolicitud;
import RpcDonaciones.entities.ItemOfertaDonacion;
import RpcDonaciones.repositories.IOfertaSolicitud;
import RpcDonaciones.kafka.messages.OfertaDonacionMessage;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OfertaDonacionConsumer {

    @Autowired
    private IOfertaSolicitud ofertaSolicitudRepository;

    @KafkaListener(topics = "oferta-donaciones", groupId = "donaciones-group")
    public void consumeOfertaDonacion(OfertaDonacionMessage message) {
        try {
            // Verificar si la oferta ya existe
            Optional<OfertaSolicitud> existingOferta = ofertaSolicitudRepository
                    .findByIdOrganizacionAndIdOferta(message.getIdOrganizacion(), message.getIdOferta());

            if (existingOferta.isPresent() && !existingOferta.get().isVigente()) {
                System.out.println("Oferta ya no vigente: " + message.getIdOferta());
                return;
            }

            // Crear nueva entidad OfertaSolicitud
            OfertaSolicitud oferta = new OfertaSolicitud();
            oferta.setIdOrganizacion(message.getIdOrganizacion());
            oferta.setIdOferta(message.getIdOferta());
            oferta.setVigente(true);

            // Convertir los items del mensaje a entidades ItemOfertaDonacion
            oferta.setItemsOfertas(message.getDonaciones().stream()
                    .map(item -> new ItemOfertaDonacion(item.getCategoria(), item.getDescripcion(), item.getCantidad()))
                    .collect(Collectors.toList()));

            // Guardar en base de datos
            ofertaSolicitudRepository.save(oferta);
            System.out.println("Oferta guardada correctamente: " + message.getIdOferta());

        } catch (Exception e) {
            System.err.println("Error al procesar mensaje de oferta-donaciones: " + e.getMessage());
            e.printStackTrace();
        }
    }
}