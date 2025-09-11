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

import RpcDonaciones.repositories.IUsuario;
import RpcDonaciones.entities.Usuario;
import RpcDonaciones.entities.Rol;
import RpcDonaciones.repositories.IRol;
import RpcDonaciones.entities.enums.TipoDeRol;

@Service
public class UsuarioServiceImpl extends UsuarioServiceGrpc.UsuarioServiceImplBase {

    @Autowired
    private IUsuario userRepository;

    @Autowired
    private IRol rolRepository;

    @Override
    public void registrarUsuario(UsuarioRequest request, StreamObserver<UsuarioResponse> responseObserver) {
        try {
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombreUsuario(request.getNombreUsuario());
            nuevoUsuario.setNombre(request.getNombre());
            nuevoUsuario.setApellido(request.getApellido());
            nuevoUsuario.setTelefono(request.getTelefono());
            nuevoUsuario.setEmail(request.getEmail());
            nuevoUsuario.setClave(request.getClave());
            nuevoUsuario.setEstado(request.getActivo());

            // Convertir enum de proto a TipoDeRol
            TipoDeRol tipoRol = TipoDeRol.valueOf(request.getRol().name());
            Optional<Rol> rolOpt = rolRepository.findByType(tipoRol);
            rolOpt.ifPresent(nuevoUsuario::agregarRoles);

            Usuario usuarioGuardado = userRepository.save(nuevoUsuario);

            UsuarioResponse response = UsuarioResponse.newBuilder()
                    .setId(usuarioGuardado.getId())
                    .setNombreUsuario(usuarioGuardado.getNombreUsuario())
                    .setNombre(usuarioGuardado.getNombre())
                    .setApellido(usuarioGuardado.getApellido())
                    .setTelefono(usuarioGuardado.getTelefono())
                    .setEmail(usuarioGuardado.getEmail())
                    .setRol(request.getRol())
                    .setActivo(usuarioGuardado.isEstado())
                    .setMessage("Usuario registrado exitosamente.")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void modificarUsuario(UsuarioRequest request, StreamObserver<UsuarioResponse> responseObserver) {
        try {
            Optional<Usuario> usuarioOptional = userRepository.findById(request.getId());

            if (usuarioOptional.isPresent()) {
                Usuario usuario = usuarioOptional.get();
                usuario.setNombreUsuario(request.getNombreUsuario());
                usuario.setNombre(request.getNombre());
                usuario.setApellido(request.getApellido());
                usuario.setTelefono(request.getTelefono());
                usuario.setEmail(request.getEmail());
                usuario.setClave(request.getClave());
                usuario.setEstado(request.getActivo());

                TipoDeRol tipoRol = TipoDeRol.valueOf(request.getRol().name());
                Optional<Rol> rolOpt = rolRepository.findByType(tipoRol);
                rolOpt.ifPresent(usuario::agregarRoles);

                Usuario usuarioModificado = userRepository.save(usuario);

                UsuarioResponse response = UsuarioResponse.newBuilder()
                        .setId(usuarioModificado.getId())
                        .setNombreUsuario(usuarioModificado.getNombreUsuario())
                        .setNombre(usuarioModificado.getNombre())
                        .setApellido(usuarioModificado.getApellido())
                        .setTelefono(usuarioModificado.getTelefono())
                        .setEmail(usuarioModificado.getEmail())
                        .setRol(request.getRol())
                        .setActivo(usuarioModificado.isEstado())
                        .setMessage("Usuario modificado exitosamente.")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                UsuarioResponse response = UsuarioResponse.newBuilder()
                        .setMessage("Error: Usuario no encontrado.")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void eliminarUsuario(UsuarioIdRequest request, StreamObserver<EliminarUsuarioResponse> responseObserver) {
        try {
            if (userRepository.existsById(request.getId())) {
                userRepository.deleteById(request.getId());
                EliminarUsuarioResponse response = EliminarUsuarioResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Usuario eliminado exitosamente.")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                EliminarUsuarioResponse response = EliminarUsuarioResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Error: Usuario no encontrado.")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void listarUsuarios(ListarUsuariosRequest request, StreamObserver<ListarUsuariosResponse> responseObserver) {
        try {
            Iterable<Usuario> usuarios = userRepository.findAll();
            ListarUsuariosResponse.Builder responseBuilder = ListarUsuariosResponse.newBuilder();

            for (Usuario usuario : usuarios) {
                // Convertimos el rol de la entidad al enum del proto
                RpcDonaciones.grpc.UsuarioServiceProto.Rol protoRol = RpcDonaciones.grpc.UsuarioServiceProto.Rol.VOLUNTARIO; // valor
                                                                                                                             // por
                                                                                                                             // defecto
                if (usuario.getRolUsuario() != null && !usuario.getRolUsuario().isEmpty()) {
                    RpcDonaciones.entities.Rol firstRol = usuario.getRolUsuario().iterator().next();
                    switch (firstRol.getType()) { // suponer que getType() devuelve TipoDeRol
                        case PRESIDENTE:
                            protoRol = RpcDonaciones.grpc.UsuarioServiceProto.Rol.PRESIDENTE;
                            break;
                        case VOCAL:
                            protoRol = RpcDonaciones.grpc.UsuarioServiceProto.Rol.VOCAL;
                            break;
                        case COORDINADOR:
                            protoRol = RpcDonaciones.grpc.UsuarioServiceProto.Rol.COORDINADOR;
                            break;
                        case VOLUNTARIO:
                            protoRol = RpcDonaciones.grpc.UsuarioServiceProto.Rol.VOLUNTARIO;
                            break;
                    }
                }

                UsuarioResponse usuarioResponse = UsuarioResponse.newBuilder()
                        .setId(usuario.getId())
                        .setNombreUsuario(usuario.getNombreUsuario())
                        .setNombre(usuario.getNombre())
                        .setApellido(usuario.getApellido())
                        .setTelefono(usuario.getTelefono())
                        .setEmail(usuario.getEmail())
                        .setRol(protoRol)
                        .setActivo(usuario.isEstado())
                        .build();

                responseBuilder.addUsuarios(usuarioResponse);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void traerUsuarioPorId(UsuarioIdRequest request, StreamObserver<UsuarioResponse> responseObserver) {
        try {
            Optional<Usuario> usuarioOptional = userRepository.findById(request.getId());

            if (usuarioOptional.isPresent()) {
                Usuario usuario = usuarioOptional.get();
                UsuarioResponse response = UsuarioResponse.newBuilder()
                        .setId(usuario.getId())
                        .setNombreUsuario(usuario.getNombreUsuario())
                        .setNombre(usuario.getNombre())
                        .setApellido(usuario.getApellido())
                        .setTelefono(usuario.getTelefono())
                        .setEmail(usuario.getEmail())
                        .setActivo(usuario.isEstado())
                        .setMessage("Usuario encontrado.")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                UsuarioResponse response = UsuarioResponse.newBuilder()
                        .setMessage("Error: Usuario no encontrado.")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

}