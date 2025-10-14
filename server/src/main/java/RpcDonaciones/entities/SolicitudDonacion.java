package RpcDonaciones.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(
    name = "solicitud_donacion",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_solicitud"})
)
@Getter
@Setter
@NoArgsConstructor
public class SolicitudDonacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_organizacion")
    private String idOrganizacion;

    @Column(name = "id_solicitud")
    private String idSolicitud;

    @ElementCollection
    @CollectionTable(name = "solicitud_donacion_items")
    private List<ItemDonacion> items;

    private boolean vigente;
}

