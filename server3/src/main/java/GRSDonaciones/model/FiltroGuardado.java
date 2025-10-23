package GRSDonaciones.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "filtros_guardados")
public class FiltroGuardado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Usuario que guardó el filtro (puede ser email, token o ID según tu sistema)
    private String usuario;

    private String categoria;
    private Boolean eliminado;
    private String fechaDesde;
    private String fechaHasta;

    private String nombreFiltro; // para identificarlo al listar

    // Validación de categoría, igual que en FiltroDonacionInput
    public void setCategoria(String categoria) {
        if (categoria != null && !List.of("ROPA", "ALIMENTOS", "JUGUETES", "UTILES_ESCOLARES").contains(categoria)) {
            throw new IllegalArgumentException("Categoría inválida: " + categoria);
        }
        this.categoria = categoria;
    }
}
