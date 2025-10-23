package GRSDonaciones.resolvers;

import GRSDonaciones.model.FiltroGuardado;
import GRSDonaciones.model.FiltroGuardadoInput;
import GRSDonaciones.services.FiltroGuardadoService;
import GRSDonaciones.services.GrpcAuthClient;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@Controller
public class FiltroGuardadoResolver {

    private final FiltroGuardadoService filtroService;
    private final GrpcAuthClient authClient;
    private final HttpServletRequest httpRequest;

    public FiltroGuardadoResolver(
            FiltroGuardadoService filtroService,
            GrpcAuthClient authClient,
            HttpServletRequest httpRequest) {
        this.filtroService = filtroService;
        this.authClient = authClient;
        this.httpRequest = httpRequest;
    }

    // ------------------ Helpers ------------------
    private String getUsuarioDesdeToken() {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            throw new SecurityException("Falta token de autorización");
        }
        String token = authHeader.replace("Bearer ", "").trim();
        if (!authClient.validarToken(token)) {
            throw new SecurityException("Token inválido o expirado");
        }
        return authClient.obtenerUsuario(token);
    }

    // ------------------ QUERIES ------------------

    @QueryMapping
    public List<FiltroGuardado> obtenerFiltros() {
        String usuario = getUsuarioDesdeToken();
        return filtroService.obtenerFiltrosPorUsuario(usuario);
    }

    // ------------------ MUTATIONS ------------------

    @MutationMapping
    public FiltroGuardado actualizarFiltro(@Argument Long id, @Argument FiltroGuardadoInput filtro) {
    String usuario = getUsuarioDesdeToken();
    FiltroGuardado actualizado = new FiltroGuardado(
        id,
        usuario,
        filtro.getNombreFiltro(),
        filtro.getCategoria(),
        filtro.getEliminado(),
        filtro.getFechaDesde(),
        filtro.getFechaHasta()
    );
    return filtroService.actualizarFiltro(id, actualizado);
}


    @MutationMapping
    public FiltroGuardado actualizarFiltro(@Argument Long id, @Argument FiltroGuardadoInput filtro) {
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
}
