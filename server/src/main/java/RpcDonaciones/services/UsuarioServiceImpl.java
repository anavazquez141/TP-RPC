package RpcDonaciones.services;
import io.grpc.stub.StreamObserver;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import RpcDonaciones.grpc.UsuarioServiceGrpc;
import RpcDonaciones.grpc.UsuarioServiceProto.UsuarioRequest;
import RpcDonaciones.grpc.UsuarioServiceProto.UsuarioResponse;
import RpcDonaciones.grpc.UsuarioServiceProto.UsuarioIdRequest;
import RpcDonaciones.grpc.UsuarioServiceProto.EliminarUsuarioResponse;
import RpcDonaciones.grpc.UsuarioServiceProto.ListarUsuariosRequest;
import RpcDonaciones.grpc.UsuarioServiceProto.ListarUsuariosResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.entities.Usuario;

public class UsuarioServiceImpl extends UsuarioServiceGrpc.UsuarioServiceImplBase{
    @Autowired
    private IUsuario userRepository;
    public UsuarioResponse registrarUsuario(UsuarioRequest request) {
        // Crea una nueva entidad de usuario a partir de los datos de la solicitud
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setApellido(request.getApellido());
        nuevoUsuario.setEmail(request.getEmail());

        // Guarda el usuario en la base de datos
        Usuario usuarioGuardado = userRepository.save(nuevoUsuario);

        // Construye y devuelve la respuesta
        return UsuarioResponse.newBuilder()
                .setId(usuarioGuardado.getId())
                .setNombre(usuarioGuardado.getNombre())
                .setApellido(usuarioGuardado.getApellido())
                .setEmail(usuarioGuardado.getEmail())
                .setMessage("Usuario registrado exitosamente.")
                .build();
    }

    public UsuarioResponse modificarUsuario(UsuarioRequest request) {
        // Busca al usuario por su ID
        Optional<Usuario> usuarioOptional = userRepository.findById((long) request.getId());

        if (usuarioOptional.isPresent()) {
            Usuario usuario = usuarioOptional.get();
            // Actualiza los campos con los datos de la solicitud
            usuario.setNombre(request.getNombre());
            usuario.setApellido(request.getApellido());
            usuario.setEmail(request.getEmail());
            // Guarda los cambios en la base de datos
            Usuario usuarioModificado = userRepository.save(usuario);
            
            // Retorna una respuesta de éxito
            return UsuarioResponse.newBuilder()
                    .setId(usuarioModificado.getId())
                    .setNombre(usuarioModificado.getNombre())
                    .setApellido(usuarioModificado.getApellido())
                    .setEmail(usuarioModificado.getEmail())
                    .setMessage("Usuario modificado exitosamente.")
                    .build();
        } else {
            // Retorna un error si el usuario no fue encontrado
            return UsuarioResponse.newBuilder()
                    .setMessage("Error: Usuario no encontrado.")
                    .build();
        }
    }

     public EliminarUsuarioResponse eliminarUsuario(UsuarioIdRequest request) {
        // Busca al usuario por su ID
        if (userRepository.existsById(request.getId())) {
            userRepository.deleteById(request.getId());
            return EliminarUsuarioResponse.newBuilder()
                    .setMessage("Usuario eliminado exitosamente.")
                    .setMessage("Error: Usuario no encontrado.")
                    .build();
        } else {
            return EliminarUsuarioResponse.newBuilder()
                    .setMessage("Error: Usuario no encontrado.")
                    .build();
        }
    }

    public ListarUsuariosResponse listarUsuarios(ListarUsuariosRequest request) {
        // Obtiene todos los usuarios de la base de datos
        Iterable<Usuario> usuarios = userRepository.findAll();
        ListarUsuariosResponse.Builder responseBuilder = ListarUsuariosResponse.newBuilder();

        // Itera sobre los usuarios y los agrega a la respuesta
        for (Usuario usuario : usuarios) {
            UsuarioResponse usuarioResponse = UsuarioResponse.newBuilder()
                    .setId(usuario.getId())
                    .setNombre(usuario.getNombre())
                    .setApellido(usuario.getApellido())
                    .setEmail(usuario.getEmail())
                    .build();
            responseBuilder.addUsuarios(usuarioResponse);
        }

        return responseBuilder.build();
}

}
