package RpcDonaciones.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import RpcDonaciones.entities.Rol;
import RpcDonaciones.entities.Usuario;
import RpcDonaciones.entities.enums.TipoDeRol;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.repositories.IRol;

@Configuration
public class DataDePrueba {

    @Bean
    public CommandLineRunner initData(IUsuario userRepository, IRol rolRepository, PasswordEncoder passwordEncoder) {
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
        };
    }
}
