package RpcDonaciones.services;

import io.grpc.stub.StreamObserver;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import RpcDonaciones.grpc.AuthServiceGrpc;
import RpcDonaciones.grpc.AuthServiceProto.LoginRequest;
import RpcDonaciones.grpc.AuthServiceProto.LoginResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.entities.Usuario;
import javax.crypto.SecretKey;
import java.util.Base64;


@Service
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    @Autowired
    private IUsuario userRepository;
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
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(key)
                    .compact();

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
}