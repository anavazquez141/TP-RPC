package RpcDonaciones.entities;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Embeddable
@Getter
@Setter
@NoArgsConstructor

public class ItemOfertaDonacion {
    private String categoria;
    private String descripcion;
    private long cantidad;

    public ItemOfertaDonacion(String categoria, String descripcion, long cantidad){
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
    } 
}