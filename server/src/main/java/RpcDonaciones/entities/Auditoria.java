package RpcDonaciones.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import java.time.LocalDateTime;
import RpcDonaciones.entities.enums.TipoAccion;


@Entity
@Table(name = "auditoria_donacion")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "donacion_id")
    private Donacion donacion;

    @Column(nullable = false)
    private String usuario; // quien hizo la acción

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private String campoModificado;

    @Column(nullable = false)
    private String valorAnterior;

    @Column(nullable = false)
    private String valorNuevo;

    @Enumerated(EnumType.STRING)
    private TipoAccion tipoAccion;
}