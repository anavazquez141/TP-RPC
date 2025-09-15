package RpcDonaciones.repositories;

import RpcDonaciones.entities.BlacklistedToken;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IBlacklistedToken extends JpaRepository<BlacklistedToken, String> {
    boolean existsByToken(String token);
    List<BlacklistedToken> findByEmail(String email);
}
