package GRSDonaciones.repository;

import GRSDonaciones.model.FiltroGuardado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FiltroGuardadoRepository extends JpaRepository<FiltroGuardado, Long> {
    List<FiltroGuardado> findByUsuario(String usuario);
    FiltroGuardado findByUsuarioAndNombreFiltro(String usuario, String nombreFiltro);
}
