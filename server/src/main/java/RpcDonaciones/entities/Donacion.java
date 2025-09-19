package RpcDonaciones.entities;

import jakararta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.*;
import java.time.LocalDateTime;
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
    
    @ManyToOne
    @JoinColumn(name = "usuario_alta_id")
    private Usuario usuarioAlta;
    
    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;
    
    @ManyToOne
    @JoinColumn(name = "usuario_modificacion_id")
    private Usuario usuarioModificacion;
    
    @PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
    
}
