package RpcDonaciones.repositories;

import RpcDonaciones.entities.EventoExternoEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IEventoExterno extends JpaRepository<EventoExternoEntity, Long> {
    
    // Verifica si ya existe un evento externo con el mismo idEvento e idOrganizacion
    boolean existsByIdOrganizacionAndIdEvento(String idOrganizacion, String idEvento);
    
    // Busca un evento externo específico por organización y idEvento
    Optional<EventoExternoEntity> findByIdOrganizacionAndIdEvento(String idOrganizacion, String idEvento);
    
    // Lista solo los eventos vigentes
    List<EventoExternoEntity> findByVigenteTrue();
    
    // Verifica si existe un evento por su idEvento (sin importar organización)
    boolean existsByIdEvento(String idEvento);
}

