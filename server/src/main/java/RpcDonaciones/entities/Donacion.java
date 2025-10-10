package RpcDonaciones.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import RpcDonaciones.entities.enums.CategoriaDonacion;

@Entity
@Table(name = "donaciones")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Donacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaDonacion categoria;

    @Column(length = 100)
    private String descripcion;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private boolean eliminado = false;

    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;

    @Column(nullable = false)
    private String usuarioAlta;

    @OneToMany(mappedBy = "donacion") // Apunta al campo 'donacion' en Auditoria
    private List<Auditoria> auditorias = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }
}