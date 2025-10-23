package RpcDonaciones.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Entity
@Table(
    name = "transferencia_donacion",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_solicitud"})
)
@Getter
@Setter
@NoArgsConstructor
public class TransferenciaDonacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_organizacion_solicitante")
    private String idOrganizacionSolicitante;

    @Column(name = "id_solicitud")
    private String idSolicitud;

    @ElementCollection
    @CollectionTable(name = "transferencia_donacion_items")
    private List<ItemTransferencia> items;
}
