package RpcDonaciones.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "oferta_donacion")
@Getter
@Setter
@NoArgsConstructor

public class OfertaSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_organizacion")
    private String idOrganizacion;

    @Column(name = "id_oferta")
    private String idOferta;

    @ElementCollection
    @CollectionTable(name = "oferta_donacion_items")
    private List<ItemOfertaDonacion> itemsOfertas;

    private boolean vigente;
    
}
