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
            // Crear un rol
            Rol rol = new Rol(1L, TipoDeRol.PRESIDENTE );
            rolRepository.save(rol);
            Rol rol2 = new Rol(2L, TipoDeRol.VOCAL);
            rolRepository.save(rol2);
            Rol rol3 = new Rol(3L, TipoDeRol.COORDINADOR);
            rolRepository.save(rol3);
            Rol rol4 = new Rol(4L, TipoDeRol.VOLUNTARIO);
            rolRepository.save(rol4);
            // Crear un usuario
            Usuario user = new Usuario();
            user.setEmail("test@example.com");
            user.setNombreUsuario("testuser");
            user.setClave(passwordEncoder.encode("password123"));
            user.setNombre("Test");
            user.setApellido("User");
            user.setTelefono("123456789");
            user.setEstado(true); // Habilitado
            user.agregarRoles(rol);
            userRepository.save(user);

            Usuario user2 = new Usuario();
            user2.setEmail("voluntario@example.com");
            user2.setNombreUsuario("voluntario");
            user2.setClave(passwordEncoder.encode("password123"));
            user2.setNombre("Voluntario");
            user2.setApellido("Volunnteer");
            user2.setTelefono("222222222");
            user2.setEstado(true); // Habilitado
            user2.agregarRoles(rol4);
            userRepository.save(user2)
            ;
            System.out.println("Usuario de prueba creado: test@example.com con rol PRESIDENTE");
        };
    }
}