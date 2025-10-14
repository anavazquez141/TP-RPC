/*package RpcDonaciones.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import RpcDonaciones.kafka.producers.KafkaProducerService;
import RpcDonaciones.kafka.messages.AdhesionEventoMessage;
import RpcDonaciones.repositories.IAdhesionEventoRepository;
import RpcDonaciones.entities.AdhesionEvento;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

@Service
public class VoluntarioServiceImpl {
    
    @Autowired
    private KafkaProducerService kafkaProducer;
    
    @Autowired
    private IAdhesionEventoRepository adhesionRepository;
    
    public void registrarAdhesionEvento(String idOrganizador, Map<String, Object> adhesion) {
        try {
            AdhesionEventoMessage message = new AdhesionEventoMessage();
            message.setIdEvento((String) adhesion.get("idEvento"));
            message.setIdOrganizacion((String) adhesion.get("idOrganizacion"));
            message.setIdVoluntario((String) adhesion.get("idVoluntario"));
            message.setNombre((String) adhesion.get("nombre"));
            message.setApellido((String) adhesion.get("apellido"));
            message.setTelefono((String) adhesion.get("telefono"));
            message.setEmail((String) adhesion.get("email"));
            
            kafkaProducer.sendAdhesionEvento(idOrganizador, message);
            
        } catch (Exception e) {
            throw new RuntimeException("Error al registrar adhesion: " + e.getMessage(), e);
        }
    }
    
    public List<AdhesionEvento> listarVoluntariosPorEvento(String idEvento) {
        try {
            return adhesionRepository.findByIdEvento(idEvento);
        } catch (Exception e) {
            throw new RuntimeException("Error al listar voluntarios por evento: " + e.getMessage(), e);
        }
    }
    
    public List<AdhesionEvento> listarVoluntariosPorOrganizacion(String idOrganizacion) {
        try {
            return adhesionRepository.findByIdOrganizacion(idOrganizacion);
        } catch (Exception e) {
            throw new RuntimeException("Error al listar voluntarios por organizacion: " + e.getMessage(), e);
        }
    }
    
    public List<AdhesionEvento> listarTodosVoluntarios() {
        try {
            return adhesionRepository.findAll();
        } catch (Exception e) {
            throw new RuntimeException("Error al listar todos los voluntarios: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Long> obtenerEstadisticasVoluntarios() {
        try {
            List<AdhesionEvento> voluntarios = adhesionRepository.findAll();
            
            long totalVoluntarios = voluntarios.size();
            long voluntariosUnicos = voluntarios.stream()
                .map(AdhesionEvento::getIdVoluntario)
                .distinct()
                .count();
            
            Map<String, Long> estadisticas = new HashMap<>();
            estadisticas.put("totalAdhesiones", totalVoluntarios);
            estadisticas.put("voluntariosUnicos", voluntariosUnicos);
            
            return estadisticas;
            
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener estadisticas: " + e.getMessage(), e);
        }
    }
}
*/