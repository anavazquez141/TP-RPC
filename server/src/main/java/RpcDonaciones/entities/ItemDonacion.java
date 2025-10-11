package RpcDonaciones.entities;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ItemDonacion {
    private String categoria;
    private String descripcion;

    public ItemDonacion(String categoria, String descripcion) {
        this.categoria = categoria;
        this.descripcion = descripcion;
    }
}