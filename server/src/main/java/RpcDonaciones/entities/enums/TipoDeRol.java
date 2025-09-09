package RpcDonaciones.entities.enums;

public enum TipoDeRol {
    PRESIDENTE,
    VOCAL,
    COORDINADOR,
    VOLUNTARIO;

    public String getPrefixedName() {
        return "ROLE_" + this.name();
    }
    
}