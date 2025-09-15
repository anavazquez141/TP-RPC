package RpcDonaciones.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import RpcDonaciones.grpc.UsuarioServiceGrpc;
import RpcDonaciones.grpc.UsuarioServiceProto;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.repositories.IBlacklistedToken;
import RpcDonaciones.repositories.IRol;
import RpcDonaciones.entities.Usuario;
import RpcDonaciones.entities.Rol;
import RpcDonaciones.entities.enums.TipoDeRol;
import javax.crypto.SecretKey;
import java.util.Base64;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl extends UsuarioServiceGrpc.UsuarioServiceImplBase {

    @Autowired
    private IUsuario userRepository;
    @Autowired
    private IRol rolRepository;
    @Autowired
    private IBlacklistedToken blacklistedTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final SecretKey key;
    
    public UsuarioServiceImpl(@Value("${jwt.secret}") String base64Key) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key));
    }

    @Override
    public void registrarUsuario(UsuarioServiceProto.UsuarioRequest request, StreamObserver<UsuarioServiceProto.UsuarioResponse> responseObserver) {
        try {
            // Verificación de token
            String token = request.getToken();
            if (token == null || token.isEmpty()) {
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Token no proporcionado")
                    .build());
                responseObserver.onCompleted();
                return;
            }
            Claims claims;
            try {
                if (blacklistedTokenRepository.existsById(token)) {
                    System.out.println("Token en lista negra: " + token);
                    responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("Token inválido: está en la lista negra")
                        .build());
                    responseObserver.onCompleted();
                    return;
                }
                claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseClaimsJws(token)
                    .getPayload();
                System.out.println("Token válido: " + token);
            } catch (JwtException e) {
                System.out.println("Error en JWT: " + e.getMessage());
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Token inválido: " + e.getMessage())
                    .build());
                responseObserver.onCompleted();
                return;
            }

            // Verificación de rol PRESIDENTE
            List<String> roles = claims.get("roles", List.class);
            if (!roles.contains("ROLE_PRESIDENTE")) {
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("No tienes permiso para registrar usuarios")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            // Verificar si el email ya existe
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
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
            nuevoUsuario.setEstado(true);
            Rol rol = mapProtoRoleToEntityRol(request.getRol());
            nuevoUsuario.agregarRoles(rol);

            // Persistencia: Guardar el usuario en la base de datos
            Usuario usuarioGuardado = userRepository.save(nuevoUsuario);

            // Enviar respuesta
            responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                .setId(usuarioGuardado.getId())
                .setNombreUsuario(usuarioGuardado.getNombreUsuario())
                .setNombre(usuarioGuardado.getNombre())
                .setApellido(usuarioGuardado.getApellido())
                .setTelefono(usuarioGuardado.getTelefono())
                .setEmail(usuarioGuardado.getEmail())
                .setRol(mapTipoDeRolToProtoRole(usuarioGuardado.getRolUsuario().iterator().next().getType()))
                .setActivo(usuarioGuardado.isEstado())
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

    @Override
    public void modificarUsuario(UsuarioServiceProto.UsuarioRequest request, StreamObserver<UsuarioServiceProto.UsuarioResponse> responseObserver) {
        try {
            // Verificación de token
            String token = request.getToken();
            if (token == null || token.isEmpty()) {
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Token no proporcionado")
                    .build());
                responseObserver.onCompleted();
                return;
            }
            try {
                if (blacklistedTokenRepository.existsById(token)) {
                    System.out.println("Token en lista negra: " + token);
                    responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("Token inválido: está en la lista negra")
                        .build());
                    responseObserver.onCompleted();
                    return;
                }
                Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseClaimsJws(token);
                System.out.println("Token válido: " + token);
            } catch (JwtException e) {
                System.out.println("Error en JWT: " + e.getMessage());
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Token inválido: " + e.getMessage())
                    .build());
                responseObserver.onCompleted();
                return;
            }

            // Busca al usuario por su ID
            Optional<Usuario> usuarioOptional = userRepository.findById((long) request.getId());
            if (!usuarioOptional.isPresent()) {
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Usuario no encontrado")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            // Actualizar el usuario
            Usuario usuario = usuarioOptional.get();
            usuario.setNombreUsuario(request.getNombreUsuario());
            usuario.setNombre(request.getNombre());
            usuario.setApellido(request.getApellido());
            usuario.setTelefono(request.getTelefono());
            usuario.setEmail(request.getEmail());
            if (!request.getClave().isEmpty()) {
                usuario.setClave(passwordEncoder.encode(request.getClave()));
            }
            usuario.setEstado(true);
            TipoDeRol tipoRol = mapProtoRoleToTipoDeRol(request.getRol());
            Optional<Rol> rolOpt = rolRepository.findByType(tipoRol);
            if (rolOpt.isPresent()) {
                usuario.getRolUsuario().clear();
                usuario.agregarRoles(rolOpt.get());
            }

            // Persistir cambios
            Usuario usuarioModificado = userRepository.save(usuario);

            // Enviar respuesta
            responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                .setId(usuarioModificado.getId())
                .setNombreUsuario(usuarioModificado.getNombreUsuario())
                .setNombre(usuarioModificado.getNombre())
                .setApellido(usuarioModificado.getApellido())
                .setTelefono(usuarioModificado.getTelefono())
                .setEmail(usuarioModificado.getEmail())
                .setRol(mapTipoDeRolToProtoRole(usuarioModificado.getRolUsuario().iterator().next().getType()))
                .setActivo(usuarioModificado.isEstado())
                .setStatus("SUCCESS")
                .setMessage("Usuario modificado exitosamente")
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error al modificar usuario: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void eliminarUsuario(UsuarioServiceProto.UsuarioIdRequest request, StreamObserver<UsuarioServiceProto.EliminarUsuarioResponse> responseObserver) {
        try {
            // Verificación de token
            String token = request.getToken();
            if (token == null || token.isEmpty()) {
                responseObserver.onNext(UsuarioServiceProto.EliminarUsuarioResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Token no proporcionado")
                    .build());
                responseObserver.onCompleted();
                return;
            }
            try {
                if (blacklistedTokenRepository.existsById(token)) {
                    System.out.println("Token en lista negra: " + token);
                    responseObserver.onNext(UsuarioServiceProto.EliminarUsuarioResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Token inválido: está en la lista negra")
                        .build());
                    responseObserver.onCompleted();
                    return;
                }
                Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseClaimsJws(token);
                System.out.println("Token válido: " + token);
            } catch (JwtException e) {
                System.out.println("Error en JWT: " + e.getMessage());
                responseObserver.onNext(UsuarioServiceProto.EliminarUsuarioResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Token inválido: " + e.getMessage())
                    .build());
                responseObserver.onCompleted();
                return;
            }

            Optional<Usuario> usuarioOptional = userRepository.findById((long) request.getId());
            if (usuarioOptional.isPresent()) {
                Usuario usuario = usuarioOptional.get();
                usuario.setEstado(false); 
                userRepository.save(usuario); 
                responseObserver.onNext(UsuarioServiceProto.EliminarUsuarioResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Usuario dado de baja exitosamente")
                    .build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onNext(UsuarioServiceProto.EliminarUsuarioResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Usuario no encontrado")
                    .build());
                    responseObserver.onCompleted();
            }
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error al eliminar usuario: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void listarUsuarios(UsuarioServiceProto.ListarUsuariosRequest req, StreamObserver<UsuarioServiceProto.ListarUsuariosResponse> responseObserver) {
        try {
            // Verificación de token
            String token = req.getToken();
            if (token == null || token.isEmpty()) {
                responseObserver.onNext(UsuarioServiceProto.ListarUsuariosResponse.newBuilder()
                    .build());
                responseObserver.onCompleted();
                return;
            }
            if (!isTokenValid(token)) {
                responseObserver.onNext(UsuarioServiceProto.ListarUsuariosResponse.newBuilder()
                    .build());
                responseObserver.onCompleted();
                return;
            }

            System.out.println("Llamada a listarUsuarios");
            Iterable<Usuario> usuarios = userRepository.findAll();
            UsuarioServiceProto.ListarUsuariosResponse.Builder responseBuilder = UsuarioServiceProto.ListarUsuariosResponse.newBuilder();
            for (Usuario usuario : usuarios) {
                UsuarioServiceProto.UsuarioSinClave usuarioProto = UsuarioServiceProto.UsuarioSinClave.newBuilder()
                    .setId(usuario.getId().intValue())
                    .setNombreUsuario(usuario.getNombreUsuario())
                    .setNombre(usuario.getNombre())
                    .setApellido(usuario.getApellido())
                    .setTelefono(usuario.getTelefono())
                    .setEmail(usuario.getEmail())
                    .setRol(UsuarioServiceProto.Role.valueOf(usuario.getRolUsuario().iterator().next().getType().name()))
                    .setActivo(usuario.isEstado())
                    .build();
                responseBuilder.addUsuarios(usuarioProto);
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error al listar usuarios: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void traerUsuarioPorId(UsuarioServiceProto.UsuarioIdRequest req, StreamObserver<UsuarioServiceProto.UsuarioSinClave> responseObserver) {
        try {
            System.out.println("Llamada a traerUsuarioPorId: " + req.getId());
            Optional<Usuario> usuarioOptional = userRepository.findById((long) req.getId());
            if (!usuarioOptional.isPresent()) {
                responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Usuario no encontrado")
                    .asRuntimeException());
                return;
            }
            Usuario usuario = usuarioOptional.get();
            UsuarioServiceProto.UsuarioSinClave response = UsuarioServiceProto.UsuarioSinClave.newBuilder()
                .setId(usuario.getId().intValue())
                .setNombreUsuario(usuario.getNombreUsuario())
                .setNombre(usuario.getNombre())
                .setApellido(usuario.getApellido())
                .setTelefono(usuario.getTelefono())
                .setEmail(usuario.getEmail())
                .setRol(UsuarioServiceProto.Role.valueOf(usuario.getRolUsuario().iterator().next().getType().name()))
                .setActivo(usuario.isEstado())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error al obtener usuario: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void traerUsuarioPorEmail(UsuarioServiceProto.UsuarioEmailRequest req, StreamObserver<UsuarioServiceProto.UsuarioSinClave> responseObserver) {
        try {
            System.out.println("Llamada a traerUsuarioPorEmail: " + req.getEmail());
            Optional<Usuario> usuarioOptional = userRepository.findByEmail(req.getEmail());
            if (!usuarioOptional.isPresent()) {
                responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Usuario no encontrado")
                    .asRuntimeException());
                return;
            }
            Usuario usuario = usuarioOptional.get();
            UsuarioServiceProto.UsuarioSinClave response = UsuarioServiceProto.UsuarioSinClave.newBuilder()
                .setId(usuario.getId().intValue())
                .setNombreUsuario(usuario.getNombreUsuario())
                .setNombre(usuario.getNombre())
                .setApellido(usuario.getApellido())
                .setTelefono(usuario.getTelefono())
                .setEmail(usuario.getEmail())
                .setRol(UsuarioServiceProto.Role.valueOf(usuario.getRolUsuario().iterator().next().getType().name()))
                .setActivo(usuario.isEstado())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error al obtener usuario: " + e.getMessage())
                .asRuntimeException());
        }
    }

    private boolean isTokenValid(String token) {
    if (token == null || token.isEmpty()) {
        System.out.println("Token nulo o vacío");
        return false;
    }
    if (blacklistedTokenRepository.existsById(token)) {
        System.out.println("Token en lista negra: " + token);
        return false;
    }
    try {
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseClaimsJws(token);
        System.out.println("Token válido: " + token);
        return true;
    } catch (Exception e) {
        System.out.println("Error en JWT: " + e.getMessage());
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