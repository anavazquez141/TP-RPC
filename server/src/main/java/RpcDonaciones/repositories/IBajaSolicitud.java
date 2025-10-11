package RpcDonaciones.repositories;

import RpcDonaciones.entities.BajaSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IBajaSolicitud extends JpaRepository<BajaSolicitud, Long> {
    boolean existsByIdOrganizacionAndIdSolicitud(String idOrganizacion, String idSolicitud);
}