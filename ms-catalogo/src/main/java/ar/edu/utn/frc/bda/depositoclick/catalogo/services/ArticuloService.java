package ar.edu.utn.frc.bda.depositoclick.catalogo.services;

import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.ArticuloRequest;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.ArticuloResponse;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.ReglasStockResponse;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Articulo;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Categoria;
import ar.edu.utn.frc.bda.depositoclick.catalogo.repositories.ArticuloRepository;
import ar.edu.utn.frc.bda.depositoclick.catalogo.repositories.CategoriaRepository;
import ar.edu.utn.frc.bda.depositoclick.commons.dtos.PaginaResponse;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ConflictoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticuloService {

    private final ArticuloRepository articuloRepository;
    private final CategoriaRepository categoriaRepository;
    private final ValidadorAtributos validadorAtributos;
    private final ValidadorCodigoBarras validadorCodigoBarras;

    @Transactional(readOnly = true)
    public PaginaResponse<ArticuloResponse> buscar(String texto, Long categoriaId, Long rubroId, Boolean activo,
                                                   Pageable pageable) {
        String filtro = (texto == null || texto.isBlank()) ? null : texto.trim();
        return PaginaResponse.de(articuloRepository.buscar(filtro, categoriaId, rubroId, activo, pageable),
                CatalogoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ArticuloResponse obtener(Long id) {
        return CatalogoMapper.toResponse(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public ArticuloResponse obtenerPorSku(String sku) {
        return articuloRepository.findBySkuIgnoreCase(sku).map(CatalogoMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Articulo con SKU", sku));
    }

    // Lo usa el punto de venta cuando escanea un producto
    @Transactional(readOnly = true)
    public ArticuloResponse obtenerPorCodigoBarras(String codigo) {
        return articuloRepository.findByCodigoBarras(codigo).map(CatalogoMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Articulo con codigo de barras", codigo));
    }

    @Transactional
    public ArticuloResponse crear(ArticuloRequest request) {
        String sku = request.getSku().trim().toUpperCase();
        if (articuloRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflictoException("Ya existe un articulo con SKU " + sku);
        }
        validarCodigoBarras(request.getCodigoBarras());
        if (request.getCodigoBarras() != null && articuloRepository.existsByCodigoBarras(request.getCodigoBarras())) {
            throw new ConflictoException("Ya existe un articulo con el codigo de barras " + request.getCodigoBarras());
        }

        Articulo articulo = new Articulo();
        articulo.setSku(sku);
        articulo.setFechaAlta(LocalDateTime.now());
        articulo.setActivo(true);
        articulo.setUnidadMedida(request.getUnidadMedida());
        completar(articulo, request);
        return CatalogoMapper.toResponse(articuloRepository.save(articulo));
    }

    /**
     * Reglas de modificacion:
     * - el SKU es inmutable (otros servicios y documentos lo referencian)
     * - la unidad de medida es inmutable: si hay 10 UNIDADES en stock y se cambia a KILOGRAMO,
     *   el stock existente pasaria a significar otra cosa.
     */
    @Transactional
    public ArticuloResponse actualizar(Long id, ArticuloRequest request) {
        Articulo articulo = buscarEntidad(id);
        if (!articulo.getSku().equalsIgnoreCase(request.getSku().trim())) {
            throw new ReglaNegocioException("El SKU de un articulo no se puede modificar");
        }
        if (articulo.getUnidadMedida() != request.getUnidadMedida()) {
            throw new ReglaNegocioException("La unidad de medida no se puede modificar: el stock existente quedaria "
                    + "expresado en otra unidad. De de baja el articulo y cree uno nuevo");
        }
        validarCodigoBarras(request.getCodigoBarras());
        if (request.getCodigoBarras() != null && articuloRepository.existsByCodigoBarrasAndIdNot(request.getCodigoBarras(), id)) {
            throw new ConflictoException("Ya existe otro articulo con el codigo de barras " + request.getCodigoBarras());
        }
        completar(articulo, request);
        articulo.setFechaModificacion(LocalDateTime.now());
        return CatalogoMapper.toResponse(articuloRepository.save(articulo));
    }

    // Baja logica: el articulo deja de poder recibir stock ni comprarse, pero su historia se conserva
    @Transactional
    public void desactivar(Long id) {
        Articulo articulo = buscarEntidad(id);
        articulo.setActivo(false);
        articulo.setFechaModificacion(LocalDateTime.now());
        articuloRepository.save(articulo);
    }

    @Transactional
    public ArticuloResponse reactivar(Long id) {
        Articulo articulo = buscarEntidad(id);
        articulo.setActivo(true);
        articulo.setFechaModificacion(LocalDateTime.now());
        return CatalogoMapper.toResponse(articuloRepository.save(articulo));
    }

    @Transactional(readOnly = true)
    public ReglasStockResponse obtenerReglas(Long id) {
        return CatalogoMapper.toReglas(buscarEntidad(id));
    }

    // Version "en lote" para no hacer N llamadas desde otro microservicio. Los ids inexistentes se omiten.
    @Transactional(readOnly = true)
    public List<ReglasStockResponse> obtenerReglas(List<Long> ids) {
        if (ids.size() > 500) {
            throw new ReglaNegocioException("Se pueden consultar como maximo 500 articulos por vez");
        }
        return articuloRepository.findAllById(ids).stream().map(CatalogoMapper::toReglas).toList();
    }

    private void completar(Articulo articulo, ArticuloRequest request) {
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ReglaNegocioException("La categoria " + request.getCategoriaId() + " no existe"));

        BigDecimal stockMinimo = request.getStockMinimo();
        if (!request.getUnidadMedida().isPermiteDecimales() && stockMinimo.stripTrailingZeros().scale() > 0) {
            throw new ReglaNegocioException("El stock minimo debe ser entero para articulos que se miden en "
                    + request.getUnidadMedida());
        }

        articulo.setCodigoBarras(request.getCodigoBarras());
        articulo.setNombre(request.getNombre().trim());
        articulo.setDescripcion(request.getDescripcion());
        articulo.setCategoria(categoria);
        articulo.setStockMinimo(stockMinimo);
        articulo.reemplazarValores(validadorAtributos.validar(categoria, request.getAtributos()));
    }

    private void validarCodigoBarras(String codigo) {
        if (codigo != null && !validadorCodigoBarras.esValido(codigo)) {
            throw new ReglaNegocioException("El codigo de barras " + codigo + " tiene un digito verificador incorrecto");
        }
    }

    Articulo buscarEntidad(Long id) {
        return articuloRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Articulo", id));
    }
}
