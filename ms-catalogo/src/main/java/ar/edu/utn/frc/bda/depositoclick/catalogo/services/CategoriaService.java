package ar.edu.utn.frc.bda.depositoclick.catalogo.services;

import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.AtributoDTO;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.CategoriaRequest;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.CategoriaResponse;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.AtributoCategoria;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Categoria;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Rubro;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.TipoAtributo;
import ar.edu.utn.frc.bda.depositoclick.catalogo.repositories.ArticuloRepository;
import ar.edu.utn.frc.bda.depositoclick.catalogo.repositories.CategoriaRepository;
import ar.edu.utn.frc.bda.depositoclick.catalogo.repositories.RubroRepository;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ConflictoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final RubroRepository rubroRepository;
    private final ArticuloRepository articuloRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(Long rubroId) {
        List<Categoria> categorias = rubroId == null
                ? categoriaRepository.findAll(Sort.by("nombre"))
                : categoriaRepository.findByRubroId(rubroId);
        return categorias.stream().map(CatalogoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CatalogoMapper.toResponse(buscarEntidad(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        Rubro rubro = rubroRepository.findById(request.getRubroId())
                .orElseThrow(() -> new ReglaNegocioException("El rubro " + request.getRubroId() + " no existe"));
        if (categoriaRepository.existsByRubroIdAndNombreIgnoreCase(rubro.getId(), request.getNombre().trim())) {
            throw new ConflictoException("Ya existe la categoria '" + request.getNombre() + "' en el rubro " + rubro.getNombre());
        }
        // La fecha de vencimiento es una propiedad del lote: sin lote no hay forma de separar
        // en el deposito la mercaderia que vence en marzo de la que vence en junio.
        if (request.isControlaVencimiento() && !request.isControlaLote()) {
            throw new ReglaNegocioException("Una categoria que controla vencimiento tambien debe controlar lote");
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(request.getNombre().trim());
        categoria.setRubro(rubro);
        categoria.setControlaLote(request.isControlaLote());
        categoria.setControlaVencimiento(request.isControlaVencimiento());

        Set<String> nombres = new HashSet<>();
        for (AtributoDTO dto : request.getAtributos()) {
            if (!nombres.add(dto.getNombre().toLowerCase())) {
                throw new ReglaNegocioException("El atributo '" + dto.getNombre() + "' esta repetido");
            }
            categoria.agregarAtributo(armarAtributo(dto));
        }
        return CatalogoMapper.toResponse(categoriaRepository.save(categoria));
    }

    /**
     * Agrega un atributo a una categoria existente.
     * Regla: si la categoria ya tiene articulos, NO se puede agregar un atributo obligatorio,
     * porque esos articulos quedarian invalidos (no tienen valor para ese atributo).
     */
    @Transactional
    public CategoriaResponse agregarAtributo(Long categoriaId, AtributoDTO dto) {
        Categoria categoria = buscarEntidad(categoriaId);
        boolean repetido = categoria.getAtributos().stream()
                .anyMatch(a -> a.getNombre().equalsIgnoreCase(dto.getNombre()));
        if (repetido) {
            throw new ConflictoException("La categoria ya tiene un atributo llamado '" + dto.getNombre() + "'");
        }
        if (dto.isObligatorio() && articuloRepository.existsByCategoriaId(categoriaId)) {
            throw new ReglaNegocioException("No se puede agregar el atributo obligatorio '" + dto.getNombre()
                    + "' porque la categoria ya tiene articulos (quedarian sin valor). Agreguelo como opcional");
        }
        categoria.agregarAtributo(armarAtributo(dto));
        return CatalogoMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarEntidad(id);
        if (articuloRepository.existsByCategoriaId(id)) {
            throw new ConflictoException("No se puede eliminar la categoria '" + categoria.getNombre() + "' porque tiene articulos");
        }
        categoriaRepository.delete(categoria);
    }

    Categoria buscarEntidad(Long id) {
        return categoriaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Categoria", id));
    }

    private AtributoCategoria armarAtributo(AtributoDTO dto) {
        List<String> opciones = dto.getOpciones() == null ? List.of()
                : dto.getOpciones().stream().filter(o -> o != null && !o.isBlank()).map(String::trim).toList();
        if (dto.getTipo() == TipoAtributo.OPCION) {
            if (opciones.size() < 2) {
                throw new ReglaNegocioException("El atributo '" + dto.getNombre() + "' es de tipo OPCION y necesita al menos 2 opciones");
            }
            if (opciones.stream().anyMatch(o -> o.contains(","))) {
                throw new ReglaNegocioException("Las opciones no pueden contener comas");
            }
        } else if (!opciones.isEmpty()) {
            throw new ReglaNegocioException("Solo los atributos de tipo OPCION pueden tener opciones");
        }
        return new AtributoCategoria(dto.getNombre(), dto.getTipo(), dto.isObligatorio(),
                opciones.isEmpty() ? null : String.join(",", opciones));
    }
}
