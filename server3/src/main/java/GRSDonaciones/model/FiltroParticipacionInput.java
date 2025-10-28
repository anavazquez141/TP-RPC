package GRSDonaciones.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiltroParticipacionInput {
    private String fechaDesde;
    private String fechaHasta;
    private String usuario;  // obligatorio
}