package GRSDonaciones.resolvers;

import org.springframework.stereotype.Component;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

import GRSDonaciones.grpc.DonacionServiceGrpc;
import GRSDonaciones.grpc.DonacionServiceProto.*;
import GRSDonaciones.dto.DonacionResumen;
import GRSDonaciones.dto.FiltroDonacionInput;

@Component
public class DonacionQueryResolver {

    private final DonacionServiceGrpc.DonacionServiceBlockingStub donacionStub;

    public DonacionQueryResolver(DonacionServiceGrpc.DonacionServiceBlockingStub donacionStub) {
        this.donacionStub = donacionStub;
    }

    @QueryMapping
    public List<DonacionResumen> informeDonaciones(
            @Argument String token,
            @Argument FiltroDonacionInput filtro) {
        try {
            System.out.println("Iniciando informeDonaciones con token: " + token);
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

            // Lista de categorías válidas según el esquema GraphQL
            List<String> validCategories = List.of("ROPA", "ALIMENTOS", "JUGUETES", "UTILES_ESCOLARES");

            List<DonacionResumen> result = response.getDonacionesList().stream()
                .filter(d -> {
                    try {
                        // Validar que la categoría sea válida
                        if (!validCategories.contains(d.getCategoria())) {
                            System.out.println("Categoría inválida encontrada: " + d.getCategoria());
                            return false;
                        }
                        // Filtrar por categoria
                        return filtro == null || filtro.getCategoria() == null || 
                               d.getCategoria().equals(filtro.getCategoria());
                    } catch (Exception e) {
                        System.out.println("Error al procesar categoría: " + d.getCategoria() + ", error: " + e.getMessage());
                        return false;
                    }
                })
                .filter(d -> filtro == null || filtro.getEliminado() == null || 
                             d.getEliminado() == filtro.getEliminado())
                .filter(d -> filtro == null || filtro.getFechaDesde() == null || 
                             (d.getFechaAlta() != null && 
                              d.getFechaAlta().compareTo(filtro.getFechaDesde()) >= 0))
                .filter(d -> filtro == null || filtro.getFechaHasta() == null || 
                             (d.getFechaEliminacion() != null && 
                              d.getFechaEliminacion().compareTo(filtro.getFechaHasta()) <= 0))
                .collect(Collectors.groupingBy(
                    d -> d.getCategoria() + "|" + d.getEliminado(),
                    Collectors.summingInt(DonacionConCampoEliminado::getCantidad)
                ))
                .entrySet().stream()
                .map(e -> {
                    try {
                        String[] parts = e.getKey().split("\\|");
                        String categoria = parts[0];
                        if (!validCategories.contains(categoria)) {
                            System.out.println("Categoría inválida en mapeo: " + categoria);
                            return null;
                        }
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

    @QueryMapping
    public String testResolver() {
        System.out.println("Test resolver ejecutado");
        return "Resolver está funcionando";
    }
}