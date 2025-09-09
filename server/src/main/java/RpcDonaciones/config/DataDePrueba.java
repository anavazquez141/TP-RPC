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
            Rol rol = new Rol(1L, TipoDeRol.VOLUNTARIO);
            rolRepository.save(rol);

            // Crear un usuario
            Usuario user = new Usuario();
            user.setEmail("test@example.com");
            user.setClave(passwordEncoder.encode("password123"));
            user.setNombre("Test");
            user.setApellido("User");
            user.setTelefono("123456789");
            user.setEstado(true); // Habilitado
            user.agregarRoles(rol);
            userRepository.save(user);

            System.out.println("Usuario de prueba creado: test@example.com con rol VOLUNTARIO");
        };
    }
}