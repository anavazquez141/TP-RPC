package GRSDonaciones.controllers;

import GRSDonaciones.model.DonacionExcel;
import GRSDonaciones.model.FiltroDonacionInput;
import GRSDonaciones.services.InformeDonacionesService;
import GRSDonaciones.controllers.InformeDonacionesController;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/informes")
public class InformeDonacionesController {

    private final InformeDonacionesService informeService;

    public InformeDonacionesController(InformeDonacionesService informeService) {
        this.informeService = informeService;
    }

   @GetMapping(value = "/donaciones/descargar_excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> generarInformeExcel(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "fechaDesde", required = false) String fechaDesde,
            @RequestParam(value = "fechaHasta", required = false) String fechaHasta,
            @RequestParam(value = "eliminado", required = false) String eliminado
    ) {
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
                filtro.setEliminado(null); // ninguno seleccionado
            }

            // Obtener donaciones según filtro
            List<DonacionExcel> donaciones = informeService.obtenerDonaciones(token, filtro);

            // Generar Excel
            byte[] excel = informeService.generarExcelDonaciones(donaciones);

            // Devolver archivo como attachment
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=informe_donaciones.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excel);

        } catch (Exception e) {
            // En caso de error, devolver 500 con mensaje
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generando Excel: " + e.getMessage()).getBytes());
        }
    }
}