package GRSDonaciones.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode


public class FiltroDonacionInput {
    private String nombreFiltro;
    private String categoria;
    private Boolean eliminado;
    private String fechaDesde;
    private String fechaHasta;

    // Getters y setters
    public void setCategoria(String categoria) {
        // Validar categoría
        if (categoria != null && !List.of("ROPA", "ALIMENTOS", "JUGUETES", "UTILES_ESCOLARES").contains(categoria)) {
            throw new IllegalArgumentException("Categoría inválida: " + categoria);
        }
        this.categoria = categoria;
    }

}