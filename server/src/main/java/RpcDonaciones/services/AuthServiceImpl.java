package RpcDonaciones.services;

import io.grpc.stub.StreamObserver;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import RpcDonaciones.grpc.AuthServiceGrpc;
import RpcDonaciones.grpc.AuthServiceProto.LoginRequest;
import RpcDonaciones.grpc.AuthServiceProto.LoginResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.entities.Usuario;

@Service
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    @Autowired
    private IUsuario userRepository;

    private final byte[] key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded();

    @Override
    public void login(LoginRequest req, StreamObserver<LoginResponse> responseObserver) {
        try {
            // Simulación: Busca el usuario en la base de datos
            Optional<Usuario> usuarioOptional = userRepository.findByEmail(req.getEmail());
            if (!usuarioOptional.isPresent() || !usuarioOptional.get().getPassword().equals(req.getPassword())) { // Comparación simple, hashea en producción
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
                    .signWith(Keys.hmacShaKeyFor(key))
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