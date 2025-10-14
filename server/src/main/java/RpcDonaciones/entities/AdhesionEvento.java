/*package RpcDonaciones.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Entity
@Table(name = "adhesiones_evento")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class AdhesionEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String idEvento;

    @Column(nullable = false)
    private String idOrganizacion;

    @Column(nullable = false)
    private String idVoluntario;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private String email;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaAdhesion;
    
    
}