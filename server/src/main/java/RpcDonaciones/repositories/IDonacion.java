package RpcDonaciones.repositories;

import RpcDonaciones.entities.Donacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
    
public interface IDonacion extends JpaRepository<Donacion, Long> {
    List<Donacion> findByEliminadoFalse();
    Optional<Donacion> findByIdAndEliminadoFalse(Long id);
    List<Donacion> findByCategoriaAndEliminadoFalse(String categoria);
}
