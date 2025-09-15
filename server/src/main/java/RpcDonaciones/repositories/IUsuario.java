package RpcDonaciones.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import RpcDonaciones.entities.Usuario;

public interface IUsuario extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByNombre(String username);

    @Query("SELECT u FROM Usuario u WHERE u.nombreUsuario = :nombreUsuario")
    Optional<Usuario> findByNombreUsuario(@Param("nombreUsuario") String nombreUsuario);
}
