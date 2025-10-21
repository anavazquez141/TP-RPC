package GRSDonaciones.resolvers;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.stream.Collectors;

import GRSDonaciones.grpc.DonacionServiceGrpc;
import GRSDonaciones.grpc.DonacionServiceProto.*;
import jakarta.annotation.PostConstruct;
import GRSDonaciones.dto.DonacionResumen;
import GRSDonaciones.dto.FiltroDonacionInput;

@Component
public class DonacionQueryResolver {


    private final DonacionServiceGrpc.DonacionServiceBlockingStub donacionStub;

    @Autowired
    public DonacionQueryResolver(DonacionServiceGrpc.DonacionServiceBlockingStub donacionStub) {
        this.donacionStub = donacionStub;
        System.out.println("DonacionQueryResolver creado!");
    }

    @PostConstruct
    public void init() {
        System.out.println("DonacionQueryResolver creado, stub=" + donacionStub);
    }


    @QueryMapping
    public List<DonacionResumen> informeDonaciones(
            @Argument String token,
            @Argument FiltroDonacionInput filtro) {
        try {
            System.out.println("=== Iniciando informeDonaciones ===");
            System.out.println("Token recibido: " + token);
            System.out.println("Filtro recibido: " + filtro);

            ListarDonacionesRequest request = ListarDonacionesRequest.newBuilder()
                .setToken(token)
                .build();

            System.out.println("Enviando solicitud gRPC: " + request);
            ListarDonacionesConEliminadoResponse response = donacionStub.listarDonacionesConEliminado(request);
            System.out.println("gRPC response: " + response.getDonacionesList());

            if ("FAILURE".equals(response.getStatus())) {
                System.out.println("gRPC error: " + response.getMessage());
                throw new RuntimeException(response.getMessage());
            }

            List<String> validCategories = List.of("ROPA", "ALIMENTOS", "JUGUETES", "UTILES_ESCOLARES");
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

            List<DonacionResumen> result = response.getDonacionesList().stream()
                // Filtro por categoría
                .filter(d -> {
                    if (!validCategories.contains(d.getCategoria())) {
                        System.out.println("Categoría inválida encontrada: " + d.getCategoria());
                        return false;
                    }
                    return filtro == null || filtro.getCategoria() == null ||
                        d.getCategoria().equals(filtro.getCategoria());
                })
                // Filtro por eliminado
                .filter(d -> filtro == null || filtro.getEliminado() == null || 
                            d.getEliminado() == filtro.getEliminado())
                // Filtro por fecha de alta
                .filter(d -> {
                    if (filtro == null || filtro.getFechaDesde() == null) return true;
                    try {
                        LocalDateTime fechaAlta = LocalDateTime.parse(d.getFechaAlta(), formatter);
                        LocalDateTime fechaDesde = LocalDateTime.parse(filtro.getFechaDesde(), formatter);
                        return !fechaAlta.isBefore(fechaDesde);
                    } catch (Exception e) {
                        System.out.println("Error parseando fechaDesde: " + e.getMessage());
                        return false;
                    }
                })
                // Filtro por fecha de eliminación
                .filter(d -> {
                    if (filtro == null || filtro.getFechaHasta() == null) return true;
                    if (d.getFechaEliminacion() == null || d.getFechaEliminacion().isEmpty()) return true;
                    try {
                        LocalDateTime fechaEliminacion = LocalDateTime.parse(d.getFechaEliminacion(), formatter);
                        LocalDateTime fechaHasta = LocalDateTime.parse(filtro.getFechaHasta(), formatter);
                        return !fechaEliminacion.isAfter(fechaHasta);
                    } catch (Exception e) {
                        System.out.println("Error parseando fechaHasta: " + e.getMessage());
                        return false;
                    }
                })
                // Agrupamiento por categoría + eliminado y suma de cantidades
                .collect(Collectors.groupingBy(
                    d -> d.getCategoria() + "|" + d.getEliminado(),
                    Collectors.summingInt(DonacionConCampoEliminado::getCantidad)
                ))
                .entrySet().stream()
                .map(e -> {
                    try {
                        String[] parts = e.getKey().split("\\|");
                        String categoria = parts[0];
                        if (!validCategories.contains(categoria)) return null;
                        return new DonacionResumen(
                            categoria,
                            Boolean.parseBoolean(parts[1]),
                            e.getValue()
                        );
                    } catch (Exception ex) {
                        System.out.println("Error al mapear donación: " + ex.getMessage());
                        return null;
                    }
                })
                .filter(d -> d != null)
                .collect(Collectors.toList());

            System.out.println("Resultado final: " + result);
            return result != null ? result : Collections.emptyList();

        } catch (Exception e) {
            System.out.println("Error crítico en informeDonaciones: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}