package RpcDonaciones.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import RpcDonaciones.entities.Usuario;

public interface IUsuario extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByNombre(String username);
}
