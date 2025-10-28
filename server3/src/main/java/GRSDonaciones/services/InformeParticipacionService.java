package GRSDonaciones.services;

import GRSDonaciones.model.FiltroParticipacionInput;
import GRSDonaciones.model.ParticipacionResumen;
import GRSDonaciones.grpc.EventoSolidarioServiceProto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class InformeParticipacionService {

    private final GrpcAuthClient authClient;
    private final GrpcEventosClient eventosClient;
    private final String key = "${jwt.secret}"; // Inyectar desde config

    public InformeParticipacionService(GrpcAuthClient authClient, GrpcEventosClient eventosClient) {
        this.authClient = authClient;
        this.eventosClient = eventosClient;
    }

    public List<ParticipacionResumen> obtenerInforme(String token, String usuarioFiltro,
                                                     String fechaDesde, String fechaHasta) {

        if (!authClient.validarToken(token)) {
            throw new SecurityException("Token inválido o expirado");
        }

        Claims claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();

        String usuarioActual = claims.getSubject();
        List<String> roles = claims.get("roles", List.class);
        boolean esAdmin = roles.contains("ROLE_PRESIDENTE") || roles.contains("ROLE_COORDINADOR");

        if (!esAdmin && !usuarioActual.equals(usuarioFiltro)) {
            throw new SecurityException("Solo puedes consultar tu propia participación");
        }

        List<EventoSolidarioServiceProto.ParticipacionEvento> todas =
                eventosClient.listarParticipacionesEventos(token);

        LocalDate desde = fechaDesde != null ? LocalDate.parse(fechaDesde) : null;
        LocalDate hasta = fechaHasta != null ? LocalDate.parse(fechaHasta) : null;

        var filtradas = todas.stream()
                .filter(p -> p.getUsuario().equals(usuarioFiltro))
                .filter(p -> {
                    try {
                        LocalDate fecha = LocalDate.parse(p.getFechaEvento().substring(0, 10));
                        return (desde == null || !fecha.isBefore(desde)) &&
                               (hasta == null || !fecha.isAfter(hasta));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();

        int total = filtradas.size();

        return List.of(new ParticipacionResumen(usuarioFiltro, total));
    }
}