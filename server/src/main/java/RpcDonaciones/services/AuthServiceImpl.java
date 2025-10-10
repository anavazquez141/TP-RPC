package RpcDonaciones.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import RpcDonaciones.grpc.AuthServiceGrpc;
import RpcDonaciones.grpc.AuthServiceProto;
import RpcDonaciones.grpc.AuthServiceProto.LoginRequest;
import RpcDonaciones.grpc.AuthServiceProto.LoginResponse;
import RpcDonaciones.grpc.AuthServiceProto.LogoutRequest;
import RpcDonaciones.grpc.AuthServiceProto.LogoutResponse;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.repositories.IBlacklistedToken;
import RpcDonaciones.entities.BlacklistedToken;
import RpcDonaciones.entities.Usuario;
import javax.crypto.SecretKey;
import java.util.Base64;


@Service
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    @Autowired
    private IUsuario userRepository;
    @Autowired
    private IBlacklistedToken blacklistedTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final SecretKey key;
    private final TokenValidator tokenValidator;

    public AuthServiceImpl(@Value("${jwt.secret}") String base64Key, TokenValidator tokenValidator) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key));
        this.tokenValidator = tokenValidator;
    }

    @Override
    public void login(LoginRequest req, StreamObserver<LoginResponse> responseObserver) {
        try {
            
            String email = req.getEmail();
            String password = req.getClave();

            if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
                responseObserver.onNext(AuthServiceProto.LoginResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Email y contraseña son requeridos")
                    .build());
                responseObserver.onCompleted();
                return;
            }   

            Optional<Usuario> usuarioOptional = userRepository.findByEmail(req.getEmail());
            if (!usuarioOptional.isPresent() || !passwordEncoder.matches(req.getClave(), usuarioOptional.get().getClave())) { 
                LoginResponse response = LoginResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("Invalid email or password")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            if (usuarioOptional.get().isEstado() == false) { 
                LoginResponse response = LoginResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("Cuenta deshabilitada")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Genera un JWT
            String token = Jwts.builder()
                    .setSubject(usuarioOptional.get().getEmail())
                    .claim("roles", usuarioOptional.get().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(key)
                    .compact();

            if (blacklistedTokenRepository.existsById(token)) {
                LoginResponse response = LoginResponse.newBuilder()
                        .setStatus("FAILURE")
                        .setMessage("Token inválido generado")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            LoginResponse response = LoginResponse.newBuilder()
                    .setToken(token)
                    .setStatus("SUCCESS")
                    .setMessage("Login successful")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void logout(LogoutRequest req, StreamObserver<LogoutResponse> responseObserver) {
        try {
            String token = req.getToken();
            if (!tokenValidator.validarToken(token, responseObserver, "Auth")) {
                return;
            }
            
            String email;
            try {
                email = Jwts.parser()
                    .setSigningKey(key) // Usa la misma clave que en login
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            } catch (JwtException e) {
                // Maneja errores como token inválido o expirado
                LogoutResponse response = LogoutResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Token inválido: " + e.getMessage())
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            blacklistedTokenRepository.save(new BlacklistedToken(token, email));
            LogoutResponse response = LogoutResponse.newBuilder()
                .setStatus("SUCCESS")
                .setMessage("Logout successful")
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error en logout: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void validarToken(AuthServiceProto.TokenValidationRequest req, StreamObserver<AuthServiceProto.TokenValidationResponse> responseObserver) {
        try {
            String token = req.getToken();
            if (tokenValidator.validarToken(token, responseObserver, "Auth")) {
                responseObserver.onNext(AuthServiceProto.TokenValidationResponse.newBuilder()
                    .setStatus("SUCCESS")
                    .setMessage("Token válido")
                    .build());
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error en validación de token: " + e.getMessage())
                .asRuntimeException());
        }
    }
    

    public Usuario getUsuarioFromToken(String token) {
        String email = tokenValidator.getEmailFromToken(token);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

}