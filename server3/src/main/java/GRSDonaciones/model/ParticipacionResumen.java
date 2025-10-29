package GRSDonaciones.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipacionResumen {
    private String mes;           // "Octubre 2025"
    private int dia;              // 16
    private String nombreEvento;
    private String descripcion;
}