package RpcDonaciones.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import RpcDonaciones.entities.Auditoria;

public interface IAuditoria extends JpaRepository<Auditoria, Long> {
}
