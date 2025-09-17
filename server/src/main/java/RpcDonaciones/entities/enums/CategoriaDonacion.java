package RpcDonaciones.entities.enums;

public enum CategoriaDonacion {
   ROPA,
   ALIMENTOS,
   JUGUETES,
   UTILES_ESCOLARES;
   
   public static CategoriaDonacion fromString(String value) throws Exception{
    try {
        return CategoriaDonacion.valueOf(value.toUpperCase());
    } catch (Exception e) {
        throw new Exception("Categoría no valida" + value);
    }
   }
}

