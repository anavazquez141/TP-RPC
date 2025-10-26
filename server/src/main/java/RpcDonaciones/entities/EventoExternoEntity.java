package RpcDonaciones.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "eventos_externos",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_evento", "id_organizacion"})
)
@Getter
@Setter
@NoArgsConstructor
public class EventoExternoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID de la organización que publicó el evento
    @Column(name = "id_organizacion", nullable = false)
    private String idOrganizacion;

    // ID del evento dentro de esa organización
    @Column(name = "id_evento", nullable = false)
    private String idEvento;

    @Column(name = "nombre_evento", nullable = false)
    private String nombreEvento;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // Indicador de vigencia (para descartar los dados de baja)
    @Column(name = "vigente", nullable = false)
    private boolean vigente = true;
}
