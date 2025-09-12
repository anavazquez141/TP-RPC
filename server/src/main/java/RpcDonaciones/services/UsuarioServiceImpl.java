package RpcDonaciones.services;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import RpcDonaciones.grpc.UsuarioServiceGrpc;
import RpcDonaciones.grpc.UsuarioServiceProto;
import RpcDonaciones.grpc.UsuarioServiceProto.*;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import RpcDonaciones.repositories.IRol;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.entities.Usuario;
import RpcDonaciones.entities.Rol;
import RpcDonaciones.repositories.IRol;
import RpcDonaciones.entities.enums.TipoDeRol;

@Service
public class UsuarioServiceImpl extends UsuarioServiceGrpc.UsuarioServiceImplBase {

    @Autowired
    private IUsuario userRepository;
    @Autowired
    private IRol rolRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final java.security.Key key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);

@Override
public void registrarUsuario(UsuarioRequest request, StreamObserver<UsuarioResponse> responseObserver) {
    try {
        // Verificación de token y rol PRESIDENTE
        String token = request.getToken();
        if (token == null || token.isEmpty()) {
            responseObserver.onNext(UsuarioResponse.newBuilder()
                .setStatus("FAILURE")
                .setMessage("Token no proporcionado")
                .build());
            responseObserver.onCompleted();
            return;
        }
        if (!isTokenValid(token)) {
            responseObserver.onNext(UsuarioResponse.newBuilder()
                .setStatus("FAILURE")
                .setMessage("Token inválido")
                .build());
            responseObserver.onCompleted();
            return;
        }
        Claims claims = Jwts.parser()
            .verifyWith((SecretKey) key)
            .build()
            .parseSignedClaims(token)
            .getPayload();  // Use getPayload() instead of getBody()
        List<String> roles = claims.get("roles", List.class);
        if (!roles.contains("ROLE_PRESIDENTE")) {
            responseObserver.onNext(UsuarioResponse.newBuilder()
                .setStatus("FAILURE")
                .setMessage("No tienes permiso para registrar usuarios")
                .build());
            responseObserver.onCompleted();
            return;
        }

        // Verificar si el email ya existe
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            responseObserver.onNext(UsuarioResponse.newBuilder()
                .setStatus("FAILURE")
                .setMessage("El email ya está registrado")
                .build());
            responseObserver.onCompleted();
            return;
        }

        // Crear y persistir el usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombreUsuario(request.getNombreUsuario());
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setApellido(request.getApellido());
        nuevoUsuario.setTelefono(request.getTelefono());
        nuevoUsuario.setEmail(request.getEmail());
        nuevoUsuario.setClave(passwordEncoder.encode(request.getClave()));
        nuevoUsuario.setEstado(request.getActivo());
        Rol rol = mapProtoRoleToEntityRol(request.getRol());
        nuevoUsuario.agregarRoles(rol);

        // Persistencia: Guardar el usuario en la base de datos
        Usuario usuarioGuardado = userRepository.save(nuevoUsuario);

        // Enviar respuesta
        responseObserver.onNext(UsuarioResponse.newBuilder()
            .setId(usuarioGuardado.getId())
            .setNombreUsuario(usuarioGuardado.getNombreUsuario())
            .setNombre(usuarioGuardado.getNombre())
            .setApellido(usuarioGuardado.getApellido())
            .setTelefono(usuarioGuardado.getTelefono())
            .setEmail(usuarioGuardado.getEmail())
            .setRol(mapTipoDeRolToProtoRole(usuarioGuardado.getRolUsuario().iterator().next().getType()))
            .setActivo(usuarioGuardado.isEnabled())
            .setStatus("SUCCESS")
            .setMessage("Usuario registrado exitosamente")
            .build());
        responseObserver.onCompleted();
    } catch (Exception e) {
        responseObserver.onError(Status.INTERNAL
            .withDescription("Error al registrar usuario: " + e.getMessage())
            .asRuntimeException());
    }
}

    public void modificarUsuario(UsuarioRequest request, StreamObserver<UsuarioResponse> responseObserver) {
        // Busca al usuario por su ID
        Optional<Usuario> usuarioOptional = userRepository.findById((long) request.getId());
    try {
            if (usuarioOptional.isPresent()) {
                Usuario usuario = usuarioOptional.get();
                usuario.setNombreUsuario(request.getNombreUsuario());
                usuario.setNombre(request.getNombre());
                usuario.setApellido(request.getApellido());
                usuario.setTelefono(request.getTelefono());
                usuario.setEmail(request.getEmail());
                usuario.setClave(request.getClave());
                usuario.setEstado(request.getActivo());

                TipoDeRol tipoRol = TipoDeRol.valueOf(request.getRol().name());
                Optional<Rol> rolOpt = rolRepository.findByType(tipoRol);
                rolOpt.ifPresent(usuario::agregarRoles);

                Usuario usuarioModificado = userRepository.save(usuario);

                UsuarioResponse response = UsuarioResponse.newBuilder()
                        .setId(usuarioModificado.getId())
                        .setNombreUsuario(usuarioModificado.getNombreUsuario())
                        .setNombre(usuarioModificado.getNombre())
                        .setApellido(usuarioModificado.getApellido())
                        .setTelefono(usuarioModificado.getTelefono())
                        .setEmail(usuarioModificado.getEmail())
                        .setRol(request.getRol())
                        .setActivo(usuarioModificado.isEstado())
                        .setMessage("Usuario modificado exitosamente.")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                UsuarioResponse response = UsuarioResponse.newBuilder()
                        .setMessage("Error: Usuario no encontrado.")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void eliminarUsuario(UsuarioIdRequest request, StreamObserver<EliminarUsuarioResponse> responseObserver) {
        try {
            if (userRepository.existsById(request.getId())) {
                userRepository.deleteById(request.getId());
                EliminarUsuarioResponse response = EliminarUsuarioResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Usuario eliminado exitosamente.")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                EliminarUsuarioResponse response = EliminarUsuarioResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Error: Usuario no encontrado.")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void listarUsuarios(ListarUsuariosRequest request, StreamObserver<ListarUsuariosResponse> responseObserver) {
        try {
            Iterable<Usuario> usuarios = userRepository.findAll();
            ListarUsuariosResponse.Builder responseBuilder = ListarUsuariosResponse.newBuilder();

            for (Usuario usuario : usuarios) {
                // Convertimos el rol de la entidad al enum del proto
                RpcDonaciones.grpc.UsuarioServiceProto.Role protoRol = RpcDonaciones.grpc.UsuarioServiceProto.Role.VOLUNTARIO; // valor
                                                                                                                             // por
                                                                                                                             // defecto
                if (usuario.getRolUsuario() != null && !usuario.getRolUsuario().isEmpty()) {
                    RpcDonaciones.entities.Rol firstRol = usuario.getRolUsuario().iterator().next();
                    switch (firstRol.getType()) { // suponer que getType() devuelve TipoDeRol
                        case PRESIDENTE:
                            protoRol = RpcDonaciones.grpc.UsuarioServiceProto.Role.PRESIDENTE;
                            break;
                        case VOCAL:
                            protoRol = RpcDonaciones.grpc.UsuarioServiceProto.Role.VOCAL;
                            break;
                        case COORDINADOR:
                            protoRol = RpcDonaciones.grpc.UsuarioServiceProto.Role.COORDINADOR;
                            break;
                        case VOLUNTARIO:
                            protoRol = RpcDonaciones.grpc.UsuarioServiceProto.Role.VOLUNTARIO;
                            break;
                    }
                }

                UsuarioResponse usuarioResponse = UsuarioResponse.newBuilder()
                        .setId(usuario.getId())
                        .setNombreUsuario(usuario.getNombreUsuario() != null ? usuario.getNombreUsuario() : "")
                        .setNombre(usuario.getNombre() != null ? usuario.getNombre() : "")
                        .setApellido(usuario.getApellido() != null ? usuario.getApellido() : "")
                        .setTelefono(usuario.getTelefono() != null ? usuario.getTelefono() : "")
                        .setEmail(usuario.getEmail() != null ? usuario.getEmail() : "")
                        .setRol(protoRol)
                        .setActivo(usuario.isEstado())
                        .build();

                responseBuilder.addUsuarios(usuarioResponse);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    @Override
    public void traerUsuarioPorId(UsuarioIdRequest request, StreamObserver<UsuarioResponse> responseObserver) {
        try {
            Optional<Usuario> usuarioOptional = userRepository.findById(request.getId());

            if (usuarioOptional.isPresent()) {
                Usuario usuario = usuarioOptional.get();
                UsuarioResponse response = UsuarioResponse.newBuilder()
                        .setId(usuario.getId())
                        .setNombreUsuario(usuario.getNombreUsuario())
                        .setNombre(usuario.getNombre())
                        .setApellido(usuario.getApellido())
                        .setTelefono(usuario.getTelefono())
                        .setEmail(usuario.getEmail())
                        .setActivo(usuario.isEstado())
                        .setMessage("Usuario encontrado.")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                UsuarioResponse response = UsuarioResponse.newBuilder()
                        .setMessage("Error: Usuario no encontrado.")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }


private boolean isTokenValid(String token) {
    if (token == null || token.isEmpty()) {
        return false;
    }
    try {
        Jwts.parser()
            .verifyWith((SecretKey) key)  // Cast to SecretKey
            .build()
            .parseSignedClaims(token);  // Use parseSignedClaims instead of parseClaimsJws
        return true;
    } catch (Exception e) {
        return false;
    }
}

    private TipoDeRol mapProtoRoleToTipoDeRol(UsuarioServiceProto.Role protoRole) {
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

    private UsuarioServiceProto.Role mapTipoDeRolToProtoRole(TipoDeRol tipoDeRol) {
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

    private Rol mapProtoRoleToEntityRol(UsuarioServiceProto.Role protoRole) {
        TipoDeRol tipoDeRol = mapProtoRoleToTipoDeRol(protoRole);
        Optional<Rol> rolOptional = rolRepository.findByType(tipoDeRol);
        if (rolOptional.isPresent()) {
            return rolOptional.get();
        } else {
            Rol newRol = new Rol(null, tipoDeRol);
            return rolRepository.save(newRol);
        }
    }
}