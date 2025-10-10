package RpcDonaciones.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import RpcDonaciones.entities.Rol;
import RpcDonaciones.entities.Usuario;
import RpcDonaciones.entities.enums.TipoDeRol;
import RpcDonaciones.entities.EventoSolidario;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.repositories.IRol;
import RpcDonaciones.repositories.IEventoSolidario;


@Configuration
public class DataDePrueba {

    @Bean
    public CommandLineRunner initData(IUsuario userRepository, IRol rolRepository, PasswordEncoder passwordEncoder, IEventoSolidario eventoRepository) {
        return args -> {

            // Crear roles si no existen
            if (!rolRepository.existsById(1L)) {
                rolRepository.save(new Rol(1L, TipoDeRol.PRESIDENTE));
            }
            if (!rolRepository.existsById(2L)) {
                rolRepository.save(new Rol(2L, TipoDeRol.VOCAL));
            }
            if (!rolRepository.existsById(3L)) {
                rolRepository.save(new Rol(3L, TipoDeRol.COORDINADOR));
            }
            if (!rolRepository.existsById(4L)) {
                rolRepository.save(new Rol(4L, TipoDeRol.VOLUNTARIO));
            }

            // Recuperar roles desde DB (para evitar problemas de referencia)
            Rol rolPresidente = rolRepository.findById(1L).orElseThrow();
            Rol rolVocal = rolRepository.findById(2L).orElseThrow();
            Rol rolCoordinador = rolRepository.findById(3L).orElseThrow();
            Rol rolVoluntario = rolRepository.findById(4L).orElseThrow();

            // Crear usuario de prueba PRESIDENTE si no existe
            if (!userRepository.existsByNombreUsuario("testuser")) {
                Usuario user = new Usuario();
                user.setEmail("test@example.com");
                user.setNombreUsuario("testuser");
                user.setClave(passwordEncoder.encode("password123"));
                user.setNombre("Test");
                user.setApellido("User");
                user.setTelefono("123456789");
                user.setEstado(true); // Habilitado
                user.agregarRoles(rolPresidente);
                userRepository.save(user);
                System.out.println("Usuario de prueba creado: test@example.com con rol PRESIDENTE");
            }

            // Crear usuario de prueba VOLUNTARIO si no existe
            if (!userRepository.existsByNombreUsuario("voluntario")) {
                Usuario user2 = new Usuario();
                user2.setEmail("voluntario@example.com");
                user2.setNombreUsuario("voluntario");
                user2.setClave(passwordEncoder.encode("password123"));
                user2.setNombre("Voluntario");
                user2.setApellido("Volunteer");
                user2.setTelefono("222222222");
                user2.setEstado(true); // Habilitado
                user2.agregarRoles(rolVoluntario);
                userRepository.save(user2);
                System.out.println("Usuario de prueba creado: voluntario@example.com con rol VOLUNTARIO");
            }

            if (!userRepository.existsByNombreUsuario("coordinador")) {
                Usuario user3 = new Usuario();
                user3.setEmail("coordinador@example.com");
                user3.setNombreUsuario("coordinador");
                user3.setClave(passwordEncoder.encode("password123"));
                user3.setNombre("Coordinador");
                user3.setApellido("coordinador22");
                user3.setTelefono("222222222");
                user3.setEstado(true); // Habilitado
                user3.agregarRoles(rolCoordinador);
                userRepository.save(user3);
                System.out.println("Usuario de prueba creado: voluntario@example.com con rol COORDINADOR");
            }

            if (!userRepository.existsByNombreUsuario("vocal")) {
                Usuario user4 = new Usuario();
                user4.setEmail("vocal@example.com");
                user4.setNombreUsuario("vocal");
                user4.setClave(passwordEncoder.encode("password123"));
                user4.setNombre("Vocal");
                user4.setApellido("vocal22");
                user4.setTelefono("222222222");
                user4.setEstado(true); // Habilitado
                user4.agregarRoles(rolVocal);
                userRepository.save(user4);
                System.out.println("Usuario de prueba creado: voluntario@example.com con rol VOCAL");
            }

            // Crear eventos solidarios de prueba si no existen
            if (eventoRepository.count() == 0) {
                EventoSolidario evento1 = new EventoSolidario();
                evento1.setNombreEvento("Campaña de Invierno");
                evento1.setDescripcion("Recolección de ropa y alimentos para personas en situación de calle durante el invierno.");
                evento1.setFechaHora(LocalDateTime.parse("2025-12-16 10:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                eventoRepository.save(evento1);

                System.out.println("Evento solidario de prueba creado.");
            }
        };
    }
}
