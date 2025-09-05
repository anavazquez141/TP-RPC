package RpcDonaciones.repositories;

import RpcDonaciones.entities.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import RpcDonaciones.entities.enums.TipoDeRol;

import java.util.List;
import java.util.Optional;

@Repository
public interface IRol extends JpaRepository<Rol,Long> {
    List<Rol> findByTypeNot(TipoDeRol type);
    Optional<Rol> findByType(TipoDeRol type);
}
