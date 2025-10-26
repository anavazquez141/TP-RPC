package GRSDonaciones.controllers;

import GRSDonaciones.model.DonacionExcel;
import GRSDonaciones.model.FiltroDonacionInput;
import GRSDonaciones.services.InformeDonacionesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import GRSDonaciones.controllers.InformeDonacionesController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;



import java.util.List;

@RestController
@RequestMapping("/api/informes")
public class InformeDonacionesController {

    private static final Logger logger = LoggerFactory.getLogger(InformeDonacionesController.class);
    private final InformeDonacionesService informeService;

    public InformeDonacionesController(InformeDonacionesService informeService) {
        this.informeService = informeService;
    }

    @GetMapping(value = "/donaciones/descargar_excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<byte[]> generarInformeExcel(
            @RequestHeader(value = "Authorization", required = true) String token,
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "fechaDesde", required = false) String fechaDesde,
            @RequestParam(value = "fechaHasta", required = false) String fechaHasta,
            @RequestParam(value = "eliminado", required = false) String eliminado
    ) {
        logger.info("Recibida solicitud para generar Excel con categoria: {}, fechaDesde: {}, fechaHasta: {}, eliminado: {}",
                categoria, fechaDesde, fechaHasta, eliminado);
        try {
            // Crear filtro
            FiltroDonacionInput filtro = new FiltroDonacionInput();
            filtro.setCategoria(categoria);
            filtro.setFechaDesde(fechaDesde);
            filtro.setFechaHasta(fechaHasta);

            if ("si".equalsIgnoreCase(eliminado)) {
                filtro.setEliminado(true);
            } else if ("no".equalsIgnoreCase(eliminado)) {
                filtro.setEliminado(false);
            } else {
                filtro.setEliminado(null);
            }

            // Obtener donaciones según filtro
            List<DonacionExcel> donaciones = informeService.obtenerDonaciones(token.replace("Bearer ", ""), filtro);
            logger.info("Donaciones obtenidas: {}", donaciones.size());

            // Generar Excel
            byte[] excel = informeService.generarExcelDonaciones(donaciones);
            logger.info("Excel generado, tamaño: {} bytes", excel.length);

            // Devolver archivo como attachment
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=informe_donaciones.xlsx");
            headers.setContentLength(excel.length);

            return new ResponseEntity<>(excel, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error generando Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\": \"Error generando Excel: " + e.getMessage() + "\"}").getBytes());
        }
    }
}