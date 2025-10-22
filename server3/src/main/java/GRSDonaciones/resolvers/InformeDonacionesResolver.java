package GRSDonaciones.resolvers;

import GRSDonaciones.model.DonacionResumen;
import GRSDonaciones.model.FiltroDonacionInput;
import GRSDonaciones.services.InformeDonacionesService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class InformeDonacionesResolver {

    private final InformeDonacionesService service;
    private final HttpServletRequest httpRequest;

    public InformeDonacionesResolver(InformeDonacionesService service, HttpServletRequest httpRequest) {
        this.service = service;
        this.httpRequest = httpRequest;
    }

    @QueryMapping
    public List<DonacionResumen> informeDonaciones(@Argument FiltroDonacionInput filtro) {
        // ✅ Obtener token desde el header Authorization
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            throw new SecurityException("Falta token de autorización");
        }
        String token = authHeader.replace("Bearer ", "").trim();

        return service.obtenerInforme(
                token,
                filtro.getCategoria(),
                filtro.getFechaDesde(),
                filtro.getFechaHasta(),
                filtro.getEliminado()
        );
    }
}
