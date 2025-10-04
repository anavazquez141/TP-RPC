package RpcDonaciones.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import RpcDonaciones.repositories.IBlacklistedToken;
import RpcDonaciones.grpc.AuthServiceProto;
import RpcDonaciones.grpc.UsuarioServiceProto;
import javax.crypto.SecretKey;
import java.util.Base64;

@Component
public class TokenValidator {
    private final SecretKey key;
    private final IBlacklistedToken blacklistedTokenRepository;

    @Autowired
    public TokenValidator(@Value("${jwt.secret}") String base64Key, IBlacklistedToken blacklistedTokenRepository) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key));
        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

    public boolean validarToken(String token, StreamObserver<?> responseObserver, String tipoRespuesta) {
        if (token == null || token.isEmpty()) {
            enviarError(responseObserver, tipoRespuesta, "Token no proporcionado");
            return false;
        }
        if (blacklistedTokenRepository.existsById(token)) {
            enviarError(responseObserver, tipoRespuesta, "Token inválido: está en la lista negra");
            return false;
        }
        try {
            Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            enviarError(responseObserver, tipoRespuesta, "Token inválido: " + e.getMessage());
            return false;
        }
    }

    private void enviarError(StreamObserver<?> responseObserver, String tipoRespuesta, String mensaje) {
        if (tipoRespuesta.equals("Auth")) {
            AuthServiceProto.TokenValidationResponse response = AuthServiceProto.TokenValidationResponse.newBuilder()
                .setStatus("FAILURE")
                .setMessage(mensaje)
                .build();
            ((StreamObserver<AuthServiceProto.TokenValidationResponse>) responseObserver).onNext(response);
            responseObserver.onCompleted();
        } else if (tipoRespuesta.equals("UsuarioResponse")) {
            UsuarioServiceProto.UsuarioResponse response = UsuarioServiceProto.UsuarioResponse.newBuilder()
                .setStatus("FAILURE")
                .setMessage(mensaje)
                .build();
            ((StreamObserver<UsuarioServiceProto.UsuarioResponse>) responseObserver).onNext(response);
            responseObserver.onCompleted();
        } else if (tipoRespuesta.equals("EliminarUsuario")) {
            UsuarioServiceProto.EliminarUsuarioResponse response = UsuarioServiceProto.EliminarUsuarioResponse.newBuilder()
                .setSuccess(false)
                .setMessage(mensaje)
                .build();
            ((StreamObserver<UsuarioServiceProto.EliminarUsuarioResponse>) responseObserver).onNext(response);
            responseObserver.onCompleted();
        }
    }

    public boolean isTokenValid(String token) {
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
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            System.out.println("Token válido: " + token);
            return true;
        } catch (JwtException e) {
            System.out.println("Error en JWT: " + e.getMessage());
            return false;
        }
    }
}