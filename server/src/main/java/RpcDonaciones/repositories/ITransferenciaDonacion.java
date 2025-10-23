package RpcDonaciones.repositories;

import RpcDonaciones.entities.TransferenciaDonacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ITransferenciaDonacion extends JpaRepository<TransferenciaDonacion, Long> {
    Optional<TransferenciaDonacion> findByIdSolicitud(String idSolicitud);
}
