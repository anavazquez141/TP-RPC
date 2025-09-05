package RpcDonaciones.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import RpcDonaciones.entities.enums.TipoDeRol;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@Builder
@ToString
public class Rol {
    @Id
    @Setter(AccessLevel.NONE)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 80, unique = true)
    private TipoDeRol type;

    public Rol(Long id, @NotNull TipoDeRol type) {
        this.id = id;
        this.type = type;
    }
}
