package GRSDonaciones.resolvers;

import GRSDonaciones.model.FiltroParticipacionInput;
import GRSDonaciones.model.ParticipacionResumen;
import GRSDonaciones.services.InformeParticipacionService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class InformeParticipacionResolver {

    private final InformeParticipacionService service;
    private final HttpServletRequest httpRequest;

    public InformeParticipacionResolver(InformeParticipacionService service, HttpServletRequest httpRequest) {
        this.service = service;
        this.httpRequest = httpRequest;
    }

    @QueryMapping
    public List<ParticipacionResumen> informeParticipacionEventos(@Argument FiltroParticipacionInput filtro) {
        String token = getTokenFromHeader();

        return service.obtenerInforme(
                token,
                filtro.getUsuario(),
                filtro.getFechaDesde(),
                filtro.getFechaHasta()
        );
    }

    private String getTokenFromHeader() {
        String auth = httpRequest.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new SecurityException("Falta token de autorización");
        }
        return auth.substring(7).trim();
    }
}