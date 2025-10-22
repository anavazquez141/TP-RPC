package GRSDonaciones.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor


public class DonacionResumen {
    private String categoria;
    private boolean eliminado;
    private int totalCantidad;


    // Getters y setters
    public void setCategoria(String categoria) {
        if (categoria != null && !List.of("ROPA", "ALIMENTOS", "JUGUETES", "UTILES_ESCOLARES").contains(categoria)) {
            throw new IllegalArgumentException("Categoría inválida: " + categoria);
        }
        this.categoria = categoria;
    }

}