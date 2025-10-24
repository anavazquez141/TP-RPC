package GRSDonaciones.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonacionExcel {

    private String categoria;        // "ROPA", "ALIMENTOS", ...
    private String fechaAlta;        // formato ISO o "yyyy-MM-dd'T'HH:mm:ss"
    private String descripcion;
    private int cantidad;
    private boolean eliminado;
    private String usuarioAlta;
    private String usuarioModificacion;

    public void setCategoria(String categoria) {
        if (categoria != null && !List.of("ROPA", "ALIMENTOS", "JUGUETES", "UTILES_ESCOLARES").contains(categoria)) {
            throw new IllegalArgumentException("Categoría inválida: " + categoria);
        }
        this.categoria = categoria;
    }

}
