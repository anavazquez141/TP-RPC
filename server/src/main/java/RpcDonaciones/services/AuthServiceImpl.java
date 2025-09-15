package RpcDonaciones.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Date;
import java.util.List;
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
import RpcDonaciones.grpc.AuthServiceProto.TokenValidationResponse;
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

    public AuthServiceImpl(@Value("${jwt.secret}") String base64Key) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key));
    }

    @Override
    public void login(LoginRequest req, StreamObserver<LoginResponse> responseObserver) {
        try {
            // Simulación: Busca el usuario en la base de datos
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
            if (token == null || token.isEmpty()) {
                LogoutResponse response = LogoutResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Token no proporcionado")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
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
            if (token == null || token.isEmpty()) {
                TokenValidationResponse response = TokenValidationResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Token no proporcionado")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Verifica si el token está en la lista negra
            if (blacklistedTokenRepository.existsById(token)) {
                AuthServiceProto.TokenValidationResponse response = TokenValidationResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Token inválido: está en la lista negra")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Verifica el token JWT
            try {
                Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
                AuthServiceProto.TokenValidationResponse response = TokenValidationResponse.newBuilder()
                    .setStatus("SUCCESS")
                    .setMessage("Token válido")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (JwtException e) {
                AuthServiceProto.TokenValidationResponse response = TokenValidationResponse.newBuilder()
                    .setStatus("FAILURE")
                    .setMessage("Token inválido: " + e.getMessage())
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error en validación de token: " + e.getMessage())
                .asRuntimeException());
        }
    }
    
    public boolean isTokenValid(String token) {
        return !blacklistedTokenRepository.existsByToken(token);
    }
}


