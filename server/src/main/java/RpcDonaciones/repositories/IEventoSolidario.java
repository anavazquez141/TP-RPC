package RpcDonaciones.repositories;

import RpcDonaciones.entities.EventoSolidario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IEventoSolidario extends JpaRepository<EventoSolidario, Long> {
}