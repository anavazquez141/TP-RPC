package RpcDonaciones.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.UUID;



import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.mail.MessagingException;
import RpcDonaciones.grpc.AuthServiceProto;
import RpcDonaciones.grpc.UsuarioServiceGrpc;
import RpcDonaciones.grpc.UsuarioServiceProto;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.repositories.IBlacklistedToken;
import RpcDonaciones.repositories.IRol;
import RpcDonaciones.entities.Usuario;
import RpcDonaciones.entities.Rol;
import RpcDonaciones.entities.enums.TipoDeRol;

import javax.crypto.SecretKey;

import java.security.SecureRandom;
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
    private AuthServiceImpl authService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private IBlacklistedToken blacklistedTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final SecretKey key;
    private final TokenValidator tokenValidator;
    
    public UsuarioServiceImpl(@Value("${jwt.secret}") String base64Key, TokenValidator tokenValidator) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key));
        this.tokenValidator = tokenValidator;
    }

    @Override
    public void registrarUsuario(UsuarioServiceProto.UsuarioRequest request, StreamObserver<UsuarioServiceProto.UsuarioResponse> responseObserver) {
        try {
            String token = request.getToken();
            if (!tokenValidator.validarToken(token, responseObserver, "UsuarioResponse")) {
                return;
            }

            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseClaimsJws(token)
                .getPayload();

            List<String> roles = claims.get("roles", List.class);
            if (!roles.contains("ROLE_PRESIDENTE")) {
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("No tienes permiso para registrar usuarios")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("El email ya está registrado")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            if (userRepository.findByNombreUsuario(request.getNombreUsuario()).isPresent()) {
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("El nombre de usuario ya está registrado")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombreUsuario(request.getNombreUsuario());
            nuevoUsuario.setNombre(request.getNombre());
            nuevoUsuario.setApellido(request.getApellido());
            nuevoUsuario.setTelefono(request.getTelefono());
            nuevoUsuario.setEmail(request.getEmail());
            String clavePlana = generarClaveAleatoria(10);
            String claveEncriptada = passwordEncoder.encode(clavePlana);
            nuevoUsuario.setClave(claveEncriptada);
            nuevoUsuario.setEstado(true);
            Rol rol = mapProtoRoleToEntityRol(request.getRol());
            nuevoUsuario.agregarRoles(rol);

            Usuario usuarioGuardado = userRepository.save(nuevoUsuario);

            try {
                emailService.sendWelcomeEmail(usuarioGuardado.getEmail(), usuarioGuardado.getNombreUsuario(), clavePlana);
            } catch (MessagingException e) {
                System.out.println("Error al enviar correo: " + e.getMessage());
            }

            responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                .setId(usuarioGuardado.getId())
                .setNombreUsuario(usuarioGuardado.getNombreUsuario())
                .setNombre(usuarioGuardado.getNombre())
                .setApellido(usuarioGuardado.getApellido())
                .setTelefono(usuarioGuardado.getTelefono())
                .setEmail(usuarioGuardado.getEmail())
                .setRol(RoleMapper.mapTipoDeRolToProtoRole(usuarioGuardado.getRolUsuario().iterator().next().getType()))
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
            String token = request.getToken();
            if (!tokenValidator.validarToken(token, responseObserver, "UsuarioResponse")) {
                return;
            }

        Optional<Usuario> usuarioOptional = userRepository.findById((long) request.getId());
            if (!usuarioOptional.isPresent()) {
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Usuario no encontrado")
                    .build());
                responseObserver.onCompleted();
                return;
            }

        boolean sinCambios = usuarioOptional.get().getNombreUsuario().equals(request.getNombreUsuario()) &&
                            usuarioOptional.get().getNombre().equals(request.getNombre()) &&
                            usuarioOptional.get().getApellido().equals(request.getApellido()) &&
                            usuarioOptional.get().getTelefono().equals(request.getTelefono()) &&
                            usuarioOptional.get().getEmail().equals(request.getEmail());

        if (sinCambios) {
            responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                .setStatus("SUCCESS")
                .setMessage("No se hicieron cambios")
                .setId(usuarioOptional.get().getId().intValue())
                .setNombreUsuario(usuarioOptional.get().getNombreUsuario())
                .setNombre(usuarioOptional.get().getNombre())
                .setApellido(usuarioOptional.get().getApellido())
                .setTelefono(usuarioOptional.get().getTelefono())
                .setEmail(usuarioOptional.get().getEmail())
                .setRol(RoleMapper.mapTipoDeRolToProtoRole(usuarioOptional.get().getRolUsuario().iterator().next().getType()))
                .setActivo(usuarioOptional.get().isEstado())
                .build());
            responseObserver.onCompleted();
            return;
        }
        
            
            System.out.println("Buscando nombreUsuario: " + request.getNombreUsuario());
            Optional<Usuario> usuarioPorNombre = userRepository.findByNombreUsuario(request.getNombreUsuario());
            if (usuarioPorNombre.isPresent() && !usuarioPorNombre.get().getId().equals((long) request.getId())) {
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("El nombre de usuario ya está registrado")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            System.out.println("Buscando email: " + request.getEmail());
            Optional<Usuario> usuarioPorEmail = userRepository.findByEmail(request.getEmail());
            if (usuarioPorEmail.isPresent() && !usuarioPorEmail.get().getId().equals((long) request.getId())) {
                responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("El email ya está registrado")
                    .build());
                responseObserver.onCompleted();
                return;
            }

            Usuario usuario = usuarioOptional.get();
            usuario.setNombreUsuario(request.getNombreUsuario());
            usuario.setNombre(request.getNombre());
            usuario.setApellido(request.getApellido());
            usuario.setTelefono(request.getTelefono());
            usuario.setEmail(request.getEmail());
            TipoDeRol tipoRol = RoleMapper.mapProtoRoleToTipoDeRol(request.getRol());
            Optional<Rol> rolOpt = rolRepository.findByType(tipoRol);
            if (rolOpt.isPresent()) {
                usuario.getRolUsuario().clear();
                usuario.agregarRoles(rolOpt.get());
            }

            Usuario usuarioModificado = userRepository.save(usuario);

            responseObserver.onNext(UsuarioServiceProto.UsuarioResponse.newBuilder()
                .setId(usuarioModificado.getId())
                .setNombreUsuario(usuarioModificado.getNombreUsuario())
                .setNombre(usuarioModificado.getNombre())
                .setApellido(usuarioModificado.getApellido())
                .setTelefono(usuarioModificado.getTelefono())
                .setEmail(usuarioModificado.getEmail())
                .setRol(RoleMapper.mapTipoDeRolToProtoRole(usuarioModificado.getRolUsuario().iterator().next().getType()))
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
            String token = request.getToken();
            if (!tokenValidator.validarToken(token, responseObserver, "EliminarUsuario")) {
                return;
            }

            Optional<Usuario> usuarioOptional = userRepository.findById((long) request.getId());
            if (usuarioOptional.isPresent()) {
                Usuario usuario = usuarioOptional.get();
                usuario.setEstado(false); 
                userRepository.save(usuario); 

                AuthServiceProto.LogoutRequest logoutRequest = AuthServiceProto.LogoutRequest.newBuilder()
                    .setToken(token)
                    .build();
                StreamObserver<AuthServiceProto.LogoutResponse> logoutResponseObserver = new StreamObserver<AuthServiceProto.LogoutResponse>(){
                    @Override
                    public void onNext(AuthServiceProto.LogoutResponse response) {
                        if (response.getStatus().equals("SUCCESS")) {
                            System.out.println("Logout exitoso: " + response.getMessage());
                        } else {
                            System.out.println("Logout falló: " + response.getMessage());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error al hacer logout: " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Logout completado");
                    }
                };
                authService.logout(logoutRequest, logoutResponseObserver);
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
            String token = req.getToken();
            if (!tokenValidator.isTokenValid(token)) {
                responseObserver.onNext(UsuarioServiceProto.ListarUsuariosResponse.newBuilder().build());
                responseObserver.onCompleted();
                return;
            }

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
                    .setRol(RoleMapper.mapTipoDeRolToProtoRole(usuario.getRolUsuario().iterator().next().getType()))
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
        String token = req.getToken();
        if (!tokenValidator.isTokenValid(token)) {
            responseObserver.onError(Status.UNAUTHENTICATED
                .withDescription("Token inválido")
                .asRuntimeException());
            return;
        }

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
            .setRol(RoleMapper.mapTipoDeRolToProtoRole(usuario.getRolUsuario().iterator().next().getType()))
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
        String token = req.getToken();
        if (!tokenValidator.isTokenValid(token)) {
            responseObserver.onError(Status.UNAUTHENTICATED
                .withDescription("Token inválido")
                .asRuntimeException());
            return;
        }

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
            .setRol(RoleMapper.mapTipoDeRolToProtoRole(usuario.getRolUsuario().iterator().next().getType()))
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

    private Rol mapProtoRoleToEntityRol(UsuarioServiceProto.Role protoRole) {
        TipoDeRol tipoDeRol = RoleMapper.mapProtoRoleToTipoDeRol(protoRole);
        Optional<Rol> rolOptional = rolRepository.findByType(tipoDeRol);
        if (rolOptional.isPresent()) {
            return rolOptional.get();
        } else {
            Rol newRol = new Rol(null, tipoDeRol);
            return rolRepository.save(newRol);
        }
    }

    private String generarClaveAleatoria(int length) {
    final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#-$%";
    SecureRandom random = new SecureRandom();
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
        sb.append(chars.charAt(random.nextInt(chars.length())));
    }
    return sb.toString();
}
}