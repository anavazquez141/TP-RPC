package RpcDonaciones.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

import org.checkerframework.checker.units.qual.C;

import jakarta.persistence.Column;
import lombok.*;

@Entity
@Table(name = "blacklisted_tokens")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class BlacklistedToken {

    @Id
    private String token;

    @Column(length = 20)
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String email;

    public BlacklistedToken(String token) {
        this.token = token;
        this.email = null; // Inicializa email como vacío
        this.createdAt = LocalDateTime.now();
    }

    public BlacklistedToken(String token, String email) {
        this.token = token;
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }
}