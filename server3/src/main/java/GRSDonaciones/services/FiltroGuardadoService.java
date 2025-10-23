package GRSDonaciones.services;

import GRSDonaciones.model.FiltroGuardado;
import GRSDonaciones.repository.FiltroGuardadoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FiltroGuardadoService {

    private final FiltroGuardadoRepository repository;

    public FiltroGuardadoService(FiltroGuardadoRepository repository) {
        this.repository = repository;
    }

    // Guardar un nuevo filtro
    public FiltroGuardado guardarFiltro(FiltroGuardado filtro) {
        return repository.save(filtro);
    }

    // Obtener todos los filtros guardados de un usuario
    public List<FiltroGuardado> obtenerFiltrosPorUsuario(String usuario) {
        return repository.findByUsuario(usuario);
    }

    // Obtener un filtro guardado por nombre
    public Optional<FiltroGuardado> obtenerFiltroPorNombre(String usuario, String nombreFiltro) {
        return Optional.ofNullable(repository.findByUsuarioAndNombreFiltro(usuario, nombreFiltro));
    }

    // Eliminar un filtro guardado
    public void eliminarFiltro(Long id) {
        repository.deleteById(id);
    }

    // Editar (actualizar) un filtro existente
    public FiltroGuardado actualizarFiltro(Long id, FiltroGuardado filtroActualizado) {
        return repository.findById(id)
                .map(filtroExistente -> {
                    filtroExistente.setNombreFiltro(filtroActualizado.getNombreFiltro());
                    filtroExistente.setCategoria(filtroActualizado.getCategoria());
                    filtroExistente.setEliminado(filtroActualizado.getEliminado());
                    filtroExistente.setFechaDesde(filtroActualizado.getFechaDesde());
                    filtroExistente.setFechaHasta(filtroActualizado.getFechaHasta());
                    return repository.save(filtroExistente);
                })
                .orElseThrow(() -> new RuntimeException("Filtro no encontrado con ID: " + id));
    }
}
