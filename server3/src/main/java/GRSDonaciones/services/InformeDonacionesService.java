package GRSDonaciones.services;

import GRSDonaciones.grpc.DonacionServiceProto;
import GRSDonaciones.grpc.DonacionServiceProto.ListarDonacionesParaExcelRequest;
import GRSDonaciones.grpc.DonacionServiceProto.ListarDonacionesParaExcelResponse;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InformeDonacionesService {

    private final GrpcAuthClient authClient;
    private final GrpcDonacionesClient donacionesClient;
    private static final Logger logger = LoggerFactory.getLogger(InformeDonacionesService.class);


    public InformeDonacionesService(GrpcAuthClient authClient, GrpcDonacionesClient donacionesClient) {
        this.authClient = authClient;
        this.donacionesClient = donacionesClient;
    }

    public List<DonacionResumen> obtenerInforme(String token, String categoria,
                                                String fechaDesde, String fechaHasta,
                                                Boolean eliminado) {

        // Validar token
        if (!authClient.validarToken(token)) {
            throw new SecurityException("Token inválido o expirado");
        }

        //  Obtener todas las donaciones
        List<DonacionServiceProto.DonacionConCampoEliminado> todas = donacionesClient.listarDonacionesConEliminado(token);

        //  Convertir fechas a LocalDate
        LocalDate desde = fechaDesde != null ? LocalDate.parse(fechaDesde) : null;
        LocalDate hasta = fechaHasta != null ? LocalDate.parse(fechaHasta) : null;

        //  Filtrar opcionalmente
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

        //  Agrupar por categoría + eliminado
        Map<List<Object>, Integer> agrupado = filtradas.stream()
                .collect(Collectors.groupingBy(
                        d -> List.of(d.getCategoria(), d.getEliminado()),
                        Collectors.summingInt(DonacionServiceProto.DonacionConCampoEliminado::getCantidad)
                ));

        //  Convertir a DTO
        return agrupado.entrySet().stream()
                .map(e -> new DonacionResumen(
                        e.getKey().get(0).toString(),
                        (boolean) e.getKey().get(1),
                        e.getValue()
                ))
                .collect(Collectors.toList());
    }

    public List<DonacionExcel> obtenerDonaciones(String token, FiltroDonacionInput filtro) {
        logger.info("Obteniendo donaciones con token: {} y filtro: {}", token, filtro);
        try {
            // Crear la solicitud gRPC con los filtros
            DonacionServiceProto.ListarDonacionesParaExcelRequest request =
                    DonacionServiceProto.ListarDonacionesParaExcelRequest.newBuilder()
                            .setToken(token)
                            .setCategoria(filtro.getCategoria() != null ? filtro.getCategoria() : "")
                            .setFechaDesde(filtro.getFechaDesde() != null ? filtro.getFechaDesde() : "")
                            .setFechaHasta(filtro.getFechaHasta() != null ? filtro.getFechaHasta() : "")
                            .setEliminado(filtro.getEliminado() != null ? (filtro.getEliminado() ? "si" : "no") : "")
                            .build();

            // Llamar al servicio gRPC
            DonacionServiceProto.ListarDonacionesParaExcelResponse response =
                    donacionesClient.listarDonacionesParaExcelClient(request);

            logger.info("Donaciones recibidas de gRPC: {}", response.getDonacionesList().size());

            // Mapear la respuesta a DonacionExcel
            return response.getDonacionesList().stream().map(d -> {
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
        } catch (Exception e) {
            logger.error("Error al obtener donaciones para Excel", e);
            throw new RuntimeException("Error al obtener donaciones: " + e.getMessage());
        }
    }

    public byte[] generarExcelDonaciones(List<DonacionExcel> donaciones) throws Exception {
        logger.info("Generando Excel para {} donaciones", donaciones.size());
        donaciones.forEach(d -> logger.info("Donacion: {}", d));

        // Crear un nuevo libro de Excel
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Informe de Donaciones");

            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Categoría", "Descripción", "Cantidad", "Eliminado", "Fecha Alta", "Usuario Alta", "Usuario Modificación"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Llenar datos
            int rowNum = 1;
            for (DonacionExcel donacion : donaciones) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(donacion.getCategoria() != null ? donacion.getCategoria() : "");
                row.createCell(1).setCellValue(donacion.getDescripcion() != null ? donacion.getDescripcion() : "");
                row.createCell(2).setCellValue(donacion.getCantidad());
                row.createCell(3).setCellValue(donacion.isEliminado());
                row.createCell(4).setCellValue(donacion.getFechaAlta() != null ? donacion.getFechaAlta() : "");
                row.createCell(5).setCellValue(donacion.getUsuarioAlta() != null ? donacion.getUsuarioAlta() : "");
                row.createCell(6).setCellValue(donacion.getUsuarioModificacion() != null ? donacion.getUsuarioModificacion() : "");
            }

            // Ajustar el tamaño de las columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            try (FileOutputStream fileOut = new FileOutputStream("informe_donaciones.xlsx")) {
                workbook.write(fileOut);
            }

            // Escribir el libro en un flujo de bytes
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                byte[] excelBytes = out.toByteArray();
                logger.info("Excel generado, tamaño: {} bytes", excelBytes.length);
                return excelBytes;
            }
        } catch (Exception e) {
            logger.error("Error al generar el Excel", e);
            throw new RuntimeException("Error al generar el Excel: " + e.getMessage());
        }
    }
}


