package RpcDonaciones.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "baja_solicitud_donacion")
@Getter
@Setter
@NoArgsConstructor
public class BajaSolicitud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_organizacion")
    private String idOrganizacion;

    @Column(name = "id_solicitud")
    private String idSolicitud;
}