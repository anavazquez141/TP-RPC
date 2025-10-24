package GRSDonaciones.services;

import GRSDonaciones.grpc.DonacionServiceProto;
import GRSDonaciones.model.DonacionExcel;
import GRSDonaciones.model.DonacionResumen;
import GRSDonaciones.model.FiltroDonacionInput;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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

    public List<DonacionExcel> obtenerDonaciones(String token, FiltroDonacionInput filtro) {
        List<DonacionServiceProto.DonacionParaExcel> donacionesGrpc =
                donacionesClient.listarDonacionesParaExcel(token);

        return donacionesGrpc.stream().map(d -> {
            DonacionExcel dto = new DonacionExcel();
            dto.setCategoria(d.getCategoria());
            dto.setFechaAlta(d.getFechaAlta());
            dto.setDescripcion(d.getDescripcion());
            dto.setCantidad(d.getCantidad());
            dto.setEliminado(d.getEliminado());
            dto.setUsuarioAlta(d.getUsuarioAlta());
            dto.setUsuarioModificacion(d.getUsuarioModificacion().isEmpty() ? null : d.getUsuarioModificacion());
            return dto;
        }).toList();
    }

    // 🔹 Genera el archivo Excel con POI
    public byte[] generarExcelDonaciones(List<DonacionExcel> donaciones) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Agrupar por categoría
            Map<String, List<DonacionExcel>> porCategoria = donaciones.stream()
                .collect(Collectors.groupingBy(DonacionExcel::getCategoria));

            for (String categoria : porCategoria.keySet()) {
                Sheet sheet = workbook.createSheet(categoria);

                // Encabezados
                Row header = sheet.createRow(0);
                String[] headers = {
                    "Fecha de Alta", "Descripción", "Cantidad",
                    "Eliminado", "Usuario Alta", "Usuario Modificación"
                };

                CellStyle headerStyle = workbook.createCellStyle();
                Font boldFont = workbook.createFont();
                boldFont.setBold(true);
                headerStyle.setFont(boldFont);

                for (int i = 0; i < headers.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Filas
                int rowNum = 1;
                for (DonacionExcel d : porCategoria.get(categoria)) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(d.getFechaAlta());
                    row.createCell(1).setCellValue(d.getDescripcion());
                    row.createCell(2).setCellValue(d.getCantidad());
                    row.createCell(3).setCellValue(d.isEliminado() ? "Sí" : "No");
                    row.createCell(4).setCellValue(d.getUsuarioAlta());
                    row.createCell(5).setCellValue(
                        d.getUsuarioModificacion() != null ? d.getUsuarioModificacion() : "-"
                    );
                }

                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

}
