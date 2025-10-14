/*package RpcDonaciones.kafka.consumers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import RpcDonaciones.kafka.messages.AdhesionEventoMessage;
import RpcDonaciones.entities.AdhesionEvento;
import RpcDonaciones.repositories.IAdhesionEventoRepository;

@Component
public class AdhesionEventoConsumer {
    private static final Logger logger = LoggerFactory.getLogger(AdhesionEventoConsumer.class);
    
    @Autowired
    private IAdhesionEventoRepository adhesionRepository;
    
    @KafkaListener(topicPattern = "adhesion-evento-.*", groupId = "eventos-group")
    public void consumeAdhesionEvento(AdhesionEventoMessage message) {
        try {
            logger.info("Nueva adhesion recibida para evento {}: {} {} ({})",
                message.getIdEvento(), message.getNombre(), message.getApellido(), 
                message.getIdOrganizacion());
            
            AdhesionEvento adhesion = new AdhesionEvento();
            adhesion.setIdEvento(message.getIdEvento());
            adhesion.setIdOrganizacion(message.getIdOrganizacion());
            adhesion.setIdVoluntario(message.getIdVoluntario());
            adhesion.setNombre(message.getNombre());
            adhesion.setApellido(message.getApellido());
            adhesion.setTelefono(message.getTelefono());
            adhesion.setEmail(message.getEmail());
            adhesion.setFechaAdhesion(new java.util.Date());
            
            adhesionRepository.save(adhesion);
            
            logger.info("Adhesion procesada correctamente para evento: {}", 
                message.getIdEvento());
                
        } catch (Exception e) {
            logger.error("Error al procesar adhesion para evento {}: {}", 
                message.getIdEvento(), e.getMessage(), e);
        }
    }
}*/
