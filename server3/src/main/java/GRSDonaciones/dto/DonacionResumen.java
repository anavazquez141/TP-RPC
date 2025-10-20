package GRSDonaciones.dto;

import java.util.List;

public class DonacionResumen {
    private String categoria;
    private boolean eliminado;
    private int totalCantidad;

    public DonacionResumen(String categoria, boolean eliminado, int totalCantidad) {
        // Validar categoría
        if (categoria != null && !List.of("ROPA", "ALIMENTOS", "JUGUETES", "UTILES_ESCOLARES").contains(categoria)) {
            throw new IllegalArgumentException("Categoría inválida: " + categoria);
        }
        this.categoria = categoria;
        this.eliminado = eliminado;
        this.totalCantidad = totalCantidad;
    }

    // Getters y setters
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) {
        if (categoria != null && !List.of("ROPA", "ALIMENTOS", "JUGUETES", "UTILES_ESCOLARES").contains(categoria)) {
            throw new IllegalArgumentException("Categoría inválida: " + categoria);
        }
        this.categoria = categoria;
    }
    public boolean getEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }
    public int getTotalCantidad() { return totalCantidad; }
    public void setTotalCantidad(int totalCantidad) { this.totalCantidad = totalCantidad; }

    @Override
    public String toString() {
        return "DonacionResumen{categoria=" + categoria + 
               ", eliminado=" + eliminado + 
               ", totalCantidad=" + totalCantidad + "}";
    }
}