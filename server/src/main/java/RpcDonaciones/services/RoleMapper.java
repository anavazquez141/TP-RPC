package RpcDonaciones.services;

import RpcDonaciones.entities.enums.TipoDeRol;
import RpcDonaciones.grpc.UsuarioServiceProto;

public class RoleMapper {
    public static TipoDeRol mapProtoRoleToTipoDeRol(UsuarioServiceProto.Role protoRole) {
        switch (protoRole) {
            case PRESIDENTE:
                return TipoDeRol.PRESIDENTE;
            case VOCAL:
                return TipoDeRol.VOCAL;
            case COORDINADOR:
                return TipoDeRol.COORDINADOR;
            case VOLUNTARIO:
                return TipoDeRol.VOLUNTARIO;
            default:
                throw new IllegalArgumentException("Rol desconocido: " + protoRole);
        }
    }

    public static UsuarioServiceProto.Role mapTipoDeRolToProtoRole(TipoDeRol tipoDeRol) {
        switch (tipoDeRol) {
            case PRESIDENTE:
                return UsuarioServiceProto.Role.PRESIDENTE;
            case VOCAL:
                return UsuarioServiceProto.Role.VOCAL;
            case COORDINADOR:
                return UsuarioServiceProto.Role.COORDINADOR;
            case VOLUNTARIO:
                return UsuarioServiceProto.Role.VOLUNTARIO;
            default:
                throw new IllegalArgumentException("Tipo de rol desconocido: " + tipoDeRol);
        }
    }
}