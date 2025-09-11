package RpcDonaciones.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
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

    public BlacklistedToken(String token) {
        this.token = token;
        this.createdAt = LocalDateTime.now();
    }
}