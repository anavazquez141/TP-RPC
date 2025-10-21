package RpcDonaciones.services;

import org.springframework.stereotype.Service;

@Service
public class InventarioService {
    
    public boolean verificarYDescontarInventario(String categoria, String descripcion, long cantidad) {
        System.out.println("Descontando del inventario: " + descripcion + " - Cantidad: " + cantidad);
        return true;
    }
    
    public void agregarAlInventario(String categoria, String descripcion, long cantidad) {
        System.out.println("Agregando al inventario: " + descripcion + " - Cantidad: " + cantidad);
    }
}
