package GRSDonaciones.services;

import GRSDonaciones.grpc.EventoSolidarioServiceProto;
import GRSDonaciones.model.ParticipacionResumen;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class InformeParticipacionService {

    private final GrpcAuthClient authClient;
    private final GrpcEventosClient eventosClient;
    private final String jwtSecret;

    public InformeParticipacionService(
            GrpcAuthClient authClient,
            GrpcEventosClient eventosClient,
            @Value("${jwt.secret}") String jwtSecret) {
        this.authClient = authClient;
        this.eventosClient = eventosClient;
        this.jwtSecret = jwtSecret;
    }

    public List<ParticipacionResumen> obtenerInformeDetallado(
        String token, String usuarioFiltro, String fechaDesde, String fechaHasta) {

    // Validar token
    if (!authClient.validarToken(token)) {
        throw new SecurityException("Token inválido o expirado");
    }

    // Parsear JWT
    Claims claims = Jwts.parserBuilder()
            .setSigningKey(jwtSecret)
            .build()
            .parseClaimsJws(token)
            .getBody();

    String usuarioActual = claims.getSubject();
    List<String> roles = claims.get("roles", List.class);
    boolean esAdmin = roles.contains("ROLE_PRESIDENTE") || roles.contains("ROLE_COORDINADOR");

    if (!esAdmin && !usuarioActual.equals(usuarioFiltro)) {
        throw new SecurityException("Solo puedes consultar tu propia participación");
    }

    // Obtener todas las participaciones
    List<EventoSolidarioServiceProto.ParticipacionEvento> todas =
            eventosClient.listarParticipacionesEventos(token);

    // Convertir fechas solo si no están vacías
    final LocalDate desde;
    final LocalDate hasta;

    try {
        desde = (fechaDesde != null && !fechaDesde.trim().isEmpty())
                ? LocalDate.parse(fechaDesde.trim())
                : null;
    } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Fecha desde inválida: " + fechaDesde);
    }

    try {
        hasta = (fechaHasta != null && !fechaHasta.trim().isEmpty())
                ? LocalDate.parse(fechaHasta.trim())
                : null;
    } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Fecha hasta inválida: " + fechaHasta);
    }

    // Filtrar
    return todas.stream()
            .filter(p -> p.getUsuario().equals(usuarioFiltro))
            .filter(p -> {
                try {
                    String fechaStr = p.getFechaEvento();
                    if (fechaStr == null || fechaStr.length() < 10) return false;
                    LocalDate fecha = LocalDate.parse(fechaStr.substring(0, 10));

                    boolean desdeOk = desde == null || !fecha.isBefore(desde);
                    boolean hastaOk = hasta == null || !fecha.isAfter(hasta);
                    return desdeOk && hastaOk;
                } catch (Exception e) {
                    return false; // ignorar eventos con fecha rota
                }
            })
            .map(p -> {
                String fechaStr = p.getFechaEvento().substring(0, 10);
                LocalDate fecha = LocalDate.parse(fechaStr);
                String mes = fecha.format(DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES")));
                mes = Character.toUpperCase(mes.charAt(0)) + mes.substring(1);
                return new ParticipacionResumen(mes, fecha.getDayOfMonth(), p.getEventoNombre(), p.getEventoDescripcion());
            })
            .sorted(Comparator.comparing(ParticipacionResumen::getMes).reversed()
                    .thenComparing(ParticipacionResumen::getDia))
            .toList();
    }
}
