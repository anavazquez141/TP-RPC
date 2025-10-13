package RpcDonaciones.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import RpcDonaciones.entities.OfertaSolicitud;

import java.util.Optional;

@Repository
public interface IOfertaSolicitud extends JpaRepository<OfertaSolicitud, Long> {
    Optional<OfertaSolicitud> findByIdOrganizacionAndIdOferta(String idOrganizacion, String idOferta);
}