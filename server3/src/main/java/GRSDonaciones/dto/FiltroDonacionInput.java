package GRSDonaciones.dto;

import java.util.List;

public class FiltroDonacionInput {
    private String categoria;
    private Boolean eliminado;
    private String fechaDesde;
    private String fechaHasta;

    // Getters y setters
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) {
        // Validar categoría
        if (categoria != null && !List.of("ROPA", "ALIMENTOS", "JUGUETES", "UTILES_ESCOLARES").contains(categoria)) {
            throw new IllegalArgumentException("Categoría inválida: " + categoria);
        }
        this.categoria = categoria;
    }
    public Boolean getEliminado() { return eliminado; }
    public void setEliminado(Boolean eliminado) { this.eliminado = eliminado; }
    public String getFechaDesde() { return fechaDesde; }
    public void setFechaDesde(String fechaDesde) { this.fechaDesde = fechaDesde; }
    public String getFechaHasta() { return fechaHasta; }
    public void setFechaHasta(String fechaHasta) { this.fechaHasta = fechaHasta; }

    @Override
    public String toString() {
        return "FiltroDonacionInput{categoria=" + categoria + 
               ", eliminado=" + eliminado + 
               ", fechaDesde=" + fechaDesde + 
               ", fechaHasta=" + fechaHasta + "}";
    }
}