package RpcDonaciones.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import RpcDonaciones.services.AuthServiceImpl;
import RpcDonaciones.services.DonacionServiceImpl;
import RpcDonaciones.services.UsuarioServiceImpl;
//import RpcDonaciones.services.DonacionServiceImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class GrpcServerConfig {

    private Server server;

    @Autowired
    private AuthServiceImpl authService;

    @Autowired
    private UsuarioServiceImpl usuarioService;
    
    @Autowired
    private DonacionServiceImpl donacionService;

    @Value("${grpc.server.port}")
    private int grpcPort;

    @PostConstruct
    public void startGrpcServer() throws Exception {
        // Limpiar el puerto antes de iniciar el servidor
        System.out.println("Buscando procesos que usan el puerto " + grpcPort + "...");
        String pid = findProcessUsingPort(grpcPort);
        if (pid != null && !pid.isEmpty()) {
            System.out.println("Terminando proceso con PID " + pid + " que usa el puerto " + grpcPort + "...");
            terminateProcess(pid);
        } else {
            System.out.println("No se encontraron procesos usando el puerto " + grpcPort + ".");
        }

        // Iniciar el servidor gRPC con ambos servicios
        server = ServerBuilder.forPort(grpcPort)
                .addService(authService)
                .addService(usuarioService)
                .addService(this.donacionService)
                .build()
                .start();
        System.out.println("gRPC server started on port " + grpcPort);
    }

    @PreDestroy
    public void stopGrpcServer() {
        if (server != null) {
            System.out.println("Deteniendo el servidor gRPC en el puerto " + grpcPort + "...");
            server.shutdown();
            try {
                server.awaitTermination(); // Espera a que el servidor se detenga completamente
                System.out.println("Servidor gRPC detenido correctamente.");
            } catch (InterruptedException e) {
                System.err.println("Error al detener el servidor gRPC: " + e.getMessage());
            }
        }
    }

    private String findProcessUsingPort(int port) {
        try {
            // Usar cmd.exe para asegurar compatibilidad en Windows
            String[] command = {"cmd.exe", "/C", "netstat -aon | findstr :" + port};
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("Línea de netstat: " + line);
                if (line.contains("LISTENING")) {
                    // Extrae el PID (última columna)
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 5) {
                        String pid = parts[parts.length - 1];
                        if (isValidPid(pid)) {
                            reader.close();
                            process.destroy();
                            return pid;
                        }
                    }
                }
            }
            reader.close();
            process.destroy();
            System.out.println("Salida completa de netstat: \n" + output.toString());
        } catch (Exception e) {
            System.err.println("Error al buscar procesos en el puerto " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private boolean isValidPid(String pid) {
        try {
            return Integer.parseInt(pid) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void terminateProcess(String pid) {
        try {
            // Verifica que el proceso sea java.exe
            String[] checkCommand = {"cmd.exe", "/C", "tasklist /FI \"PID eq " + pid + "\""};
            Process checkProcess = Runtime.getRuntime().exec(checkCommand);
            BufferedReader checkReader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
            String line;
            boolean isJavaProcess = false;
            while ((line = checkReader.readLine()) != null) {
                System.out.println("Línea de tasklist: " + line);
                if (line.contains("java.exe")) {
                    isJavaProcess = true;
                    break;
                }
            }
            checkReader.close();
            checkProcess.destroy();

            if (isJavaProcess) {
                // Ejecuta taskkill /PID pid /F
                String[] command = {"cmd.exe", "/C", "taskkill /PID " + pid + " /F"};
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    System.out.println("Línea de taskkill: " + line);
                }
                reader.close();
                process.destroy();
            } else {
                System.out.println("El proceso con PID " + pid + " no es java.exe, no se terminará.");
            }
        } catch (Exception e) {
            System.err.println("Error al terminar proceso con PID " + pid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
/*package RpcDonaciones.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import RpcDonaciones.services.AuthServiceImpl;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class GrpcServerConfig {

    private Server server;

    @Autowired
    private AuthServiceImpl authService;

    @Value("${grpc.server.port}")
    private int grpcPort; // Spring inyecta aquí el puerto

    @PostConstruct
    public void startGrpcServer() throws Exception {
        server = ServerBuilder.forPort(grpcPort)
                              .addService(authService) // usa la instancia de Spring
                              .build()
                              .start();
        System.out.println("gRPC server started on port " + grpcPort);
    }

    @PreDestroy
    public void stopGrpcServer() {
        if (server != null) {
            server.shutdown();
        }
    }
}
*/
