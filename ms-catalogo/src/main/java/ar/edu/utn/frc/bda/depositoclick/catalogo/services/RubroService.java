package ar.edu.utn.frc.bda.depositoclick.catalogo.services;

import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.RubroDTO;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Rubro;
import ar.edu.utn.frc.bda.depositoclick.catalogo.repositories.CategoriaRepository;
import ar.edu.utn.frc.bda.depositoclick.catalogo.repositories.RubroRepository;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ConflictoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RubroService {

    private final RubroRepository rubroRepository;
    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<RubroDTO> listar() {
        return rubroRepository.findAll(Sort.by("nombre")).stream().map(CatalogoMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public RubroDTO obtener(Long id) {
        return CatalogoMapper.toDTO(buscarEntidad(id));
    }

    @Transactional
    public RubroDTO crear(RubroDTO dto) {
        if (rubroRepository.existsByNombreIgnoreCase(dto.getNombre().trim())) {
            throw new ConflictoException("Ya existe un rubro con el nombre '" + dto.getNombre() + "'");
        }
        Rubro rubro = Rubro.builder().nombre(dto.getNombre().trim()).descripcion(dto.getDescripcion()).build();
        return CatalogoMapper.toDTO(rubroRepository.save(rubro));
    }

    @Transactional
    public RubroDTO actualizar(Long id, RubroDTO dto) {
        Rubro rubro = buscarEntidad(id);
        if (rubroRepository.existsByNombreIgnoreCaseAndIdNot(dto.getNombre().trim(), id)) {
            throw new ConflictoException("Ya existe otro rubro con el nombre '" + dto.getNombre() + "'");
        }
        rubro.setNombre(dto.getNombre().trim());
        rubro.setDescripcion(dto.getDescripcion());
        return CatalogoMapper.toDTO(rubroRepository.save(rubro));
    }

    @Transactional
    public void eliminar(Long id) {
        Rubro rubro = buscarEntidad(id);
        if (categoriaRepository.existsByRubroId(id)) {
            throw new ConflictoException("No se puede eliminar el rubro '" + rubro.getNombre() + "' porque tiene categorias");
        }
        rubroRepository.delete(rubro);
    }

    Rubro buscarEntidad(Long id) {
        return rubroRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Rubro", id));
    }
}
