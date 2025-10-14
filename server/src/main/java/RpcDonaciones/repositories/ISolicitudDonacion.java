package RpcDonaciones.repositories;

import RpcDonaciones.entities.SolicitudDonacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ISolicitudDonacion extends JpaRepository<SolicitudDonacion, Long> {
    boolean existsByIdOrganizacionAndIdSolicitud(String idOrganizacion, String idSolicitud);
    Optional<SolicitudDonacion> findByIdOrganizacionAndIdSolicitud(String idOrganizacion, String idSolicitud);
    List<SolicitudDonacion> findByVigenteTrue();
    boolean existsByIdSolicitud(String idSolicitud);
}