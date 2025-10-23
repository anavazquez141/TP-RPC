package GRSDonaciones.resolvers;

import GRSDonaciones.model.FiltroGuardado;
import GRSDonaciones.model.FiltroDonacionInput;
import GRSDonaciones.services.FiltroGuardadoService;
import GRSDonaciones.services.GrpcAuthClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Controller
public class FiltroGuardadoResolver {

    private final FiltroGuardadoService filtroService;
    private final GrpcAuthClient authClient;
    private final HttpServletRequest httpRequest;
    private final Key jwtKey;

    public FiltroGuardadoResolver(
            FiltroGuardadoService filtroService,
            GrpcAuthClient authClient,
            HttpServletRequest httpRequest,
            @Value("${jwt.secret}") String base64Secret) {
        this.filtroService = filtroService;
        this.authClient = authClient;
        this.httpRequest = httpRequest;
        byte[] decodedKey = Base64.getDecoder().decode(base64Secret);
        this.jwtKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
    }

    // ------------------ Helpers ------------------
    private String getUsuarioDesdeToken() {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            throw new SecurityException("Falta token de autorización");
        }
        String token = authHeader.replace("Bearer ", "").trim();

        // Validar token con gRPC
        if (!authClient.validarToken(token)) {
            throw new SecurityException("Token inválido o expirado");
        }

        // Extraer email del token JWT
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject(); // email del usuario
        } catch (Exception e) {
            throw new SecurityException("Token inválido");
        }
    }

    // ------------------ QUERIES ------------------
    @QueryMapping
    public List<FiltroGuardado> obtenerFiltros() {
        String usuario = getUsuarioDesdeToken();
        return filtroService.obtenerFiltrosPorUsuario(usuario);
    }

    // ------------------ MUTATIONS ------------------
    @MutationMapping
    public FiltroGuardado actualizarFiltro(@Argument Long id, @Argument FiltroDonacionInput filtro) {
        String usuario = getUsuarioDesdeToken();
        Optional<FiltroGuardado> opt = filtroService.obtenerFiltroPorNombre(usuario, filtro.getNombreFiltro());
        if (opt.isEmpty()) {
            throw new RuntimeException("No existe filtro con ese nombre");
        }

        FiltroGuardado existente = opt.get();
        existente.setCategoria(filtro.getCategoria());
        existente.setEliminado(filtro.getEliminado());
        existente.setFechaDesde(filtro.getFechaDesde());
        existente.setFechaHasta(filtro.getFechaHasta());

        return filtroService.guardarFiltro(existente);
    }

    @MutationMapping
    public String eliminarFiltro(@Argument Long id) {
        filtroService.eliminarFiltro(id);
        return "Filtro eliminado correctamente";
    }


   @MutationMapping
    public FiltroGuardado guardarFiltro(@Argument FiltroDonacionInput filtro) {
    String usuario = getUsuarioDesdeToken();

    // Crear un nuevo FiltroGuardado
    FiltroGuardado nuevo = new FiltroGuardado();
    nuevo.setNombreFiltro(filtro.getNombreFiltro());
    nuevo.setCategoria(filtro.getCategoria());
    nuevo.setEliminado(filtro.getEliminado());
    nuevo.setFechaDesde(filtro.getFechaDesde());
    nuevo.setFechaHasta(filtro.getFechaHasta());
    nuevo.setUsuario(usuario);

    // Guardar en la base de datos
    return filtroService.guardarFiltro(nuevo);
 }
    @MutationMapping
    public FiltroGuardado aplicarFiltro(@Argument Long filtroId) {
        String usuario = getUsuarioDesdeToken();

        Optional<FiltroGuardado> opt = filtroService.obtenerFiltrosPorUsuario(usuario)
            .stream()
            .filter(f -> f.getId().equals(filtroId))
            .findFirst();

        if (opt.isEmpty()) {
            throw new RuntimeException("Filtro no encontrado");
        }

        FiltroGuardado filtro = opt.get(); // ahora es un objeto real, no un dict
        return filtro; // podés retornar al frontend o usar para filtrar tu informe
    }

}
