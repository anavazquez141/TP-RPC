package RpcDonaciones.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "evento_solidario")
@Getter
@Setter
@Data
public class EventoSolidario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEvento;

    @Column(length = 50, unique = true, nullable = false)
    private String nombreEvento;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 20)
    private LocalDateTime fechaHora;

    @ManyToMany
    @JoinTable(
        name = "evento_usuarios",
        joinColumns = @JoinColumn(name = "id_evento"),
        inverseJoinColumns = @JoinColumn(name = "id_usuario")
    )
    private List<Usuario> usuarios;
}    