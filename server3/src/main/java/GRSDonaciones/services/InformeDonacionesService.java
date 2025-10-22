package GRSDonaciones.services;

import GRSDonaciones.grpc.DonacionServiceProto;
import GRSDonaciones.model.DonacionResumen;import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InformeDonacionesService {

    private final GrpcAuthClient authClient;
    private final GrpcDonacionesClient donacionesClient;

    public InformeDonacionesService(GrpcAuthClient authClient, GrpcDonacionesClient donacionesClient) {
        this.authClient = authClient;
        this.donacionesClient = donacionesClient;
    }

    public List<DonacionResumen> obtenerInforme(String token, String categoria,
                                                String fechaDesde, String fechaHasta,
                                                Boolean eliminado) {

        // 1️⃣ Validar token
        if (!authClient.validarToken(token)) {
            throw new SecurityException("Token inválido o expirado");
        }

        // 2️⃣ Obtener todas las donaciones
        List<DonacionServiceProto.DonacionConCampoEliminado> todas = donacionesClient.listarDonacionesConEliminado(token);

        // 3️⃣ Convertir fechas a LocalDate
        LocalDate desde = fechaDesde != null ? LocalDate.parse(fechaDesde) : null;
        LocalDate hasta = fechaHasta != null ? LocalDate.parse(fechaHasta) : null;

        // 4️⃣ Filtrar opcionalmente
        var filtradas = todas.stream()
                .filter(d -> categoria == null || d.getCategoria().equalsIgnoreCase(categoria))
                .filter(d -> eliminado == null || d.getEliminado() == eliminado)
                .filter(d -> {
                    try {
                        LocalDate fecha = LocalDate.parse(d.getFechaAlta().substring(0, 10));
                        boolean desdeOk = desde == null || !fecha.isBefore(desde);
                        boolean hastaOk = hasta == null || !fecha.isAfter(hasta);
                        return desdeOk && hastaOk;
                    } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
                        return false; // ignorar registros con fecha inválida
                    }
                })
                .collect(Collectors.toList());

        // 5️⃣ Agrupar por categoría + eliminado
        Map<List<Object>, Integer> agrupado = filtradas.stream()
                .collect(Collectors.groupingBy(
                        d -> List.of(d.getCategoria(), d.getEliminado()),
                        Collectors.summingInt(DonacionServiceProto.DonacionConCampoEliminado::getCantidad)
                ));

        // 6️⃣ Convertir a DTO
        return agrupado.entrySet().stream()
                .map(e -> new DonacionResumen(
                        e.getKey().get(0).toString(),
                        (boolean) e.getKey().get(1),
                        e.getValue()
                ))
                .collect(Collectors.toList());
    }
}
