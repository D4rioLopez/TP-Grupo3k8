package ar.edu.utn.frc.bda.depositoclick.compras.services;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ConflictoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.CatalogoClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.ReglasArticuloRemoto;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.ArticuloProveedorRequest;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.ArticuloProveedorResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.ProveedorDTO;
import ar.edu.utn.frc.bda.depositoclick.compras.models.ArticuloProveedor;
import ar.edu.utn.frc.bda.depositoclick.compras.models.EstadoOrden;
import ar.edu.utn.frc.bda.depositoclick.compras.models.Proveedor;
import ar.edu.utn.frc.bda.depositoclick.compras.repositories.ArticuloProveedorRepository;
import ar.edu.utn.frc.bda.depositoclick.compras.repositories.OrdenCompraRepository;
import ar.edu.utn.frc.bda.depositoclick.compras.repositories.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ArticuloProveedorRepository articuloProveedorRepository;
    private final OrdenCompraRepository ordenRepository;
    private final CatalogoClient catalogoClient;
    private final ValidadorCuit validadorCuit;

    @Transactional(readOnly = true)
    public List<ProveedorDTO> listar(Boolean activo) {
        List<Proveedor> proveedores = activo == null
                ? proveedorRepository.findAll(Sort.by("razonSocial"))
                : proveedorRepository.findByActivoOrderByRazonSocialAsc(activo);
        return proveedores.stream().map(ComprasMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ProveedorDTO obtener(Long id) {
        return ComprasMapper.toDTO(buscar(id));
    }

    @Transactional
    public ProveedorDTO crear(ProveedorDTO dto) {
        String cuit = validadorCuit.normalizar(dto.getCuit());
        if (!validadorCuit.esValido(cuit)) {
            throw new ReglaNegocioException("El CUIT " + dto.getCuit() + " no es valido (digito verificador incorrecto)");
        }
        if (proveedorRepository.existsByCuit(cuit)) {
            throw new ConflictoException("Ya existe un proveedor con CUIT " + cuit);
        }
        Proveedor p = Proveedor.builder()
                .cuit(cuit)
                .razonSocial(dto.getRazonSocial().trim())
                .email(dto.getEmail())
                .telefono(dto.getTelefono())
                .leadTimeDiasHabiles(dto.getLeadTimeDiasHabiles())
                .activo(true)
                .build();
        return ComprasMapper.toDTO(proveedorRepository.save(p));
    }

    /** El CUIT no se modifica (identifica fiscalmente al proveedor). */
    @Transactional
    public ProveedorDTO actualizar(Long id, ProveedorDTO dto) {
        Proveedor p = buscar(id);
        if (!p.getCuit().equals(validadorCuit.normalizar(dto.getCuit()))) {
            throw new ReglaNegocioException("El CUIT de un proveedor no se puede modificar");
        }
        p.setRazonSocial(dto.getRazonSocial().trim());
        p.setEmail(dto.getEmail());
        p.setTelefono(dto.getTelefono());
        p.setLeadTimeDiasHabiles(dto.getLeadTimeDiasHabiles());
        return ComprasMapper.toDTO(proveedorRepository.save(p));
    }

    /** Baja logica. No se permite si tiene ordenes emitidas que todavia no se terminaron de recibir. */
    @Transactional
    public void desactivar(Long id) {
        Proveedor p = buscar(id);
        if (ordenRepository.existsByProveedorIdAndEstadoIn(id, EnumSet.of(EstadoOrden.EMITIDA, EstadoOrden.RECIBIDA_PARCIAL))) {
            throw new ConflictoException("El proveedor " + p.getRazonSocial()
                    + " tiene ordenes de compra pendientes de recepcion: recibalas o cancelelas antes de darlo de baja");
        }
        p.setActivo(false);
        proveedorRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<ArticuloProveedorResponse> listarArticulos(Long proveedorId) {
        buscar(proveedorId);
        return articuloProveedorRepository.findByProveedorIdOrderBySkuAsc(proveedorId).stream()
                .map(ComprasMapper::toResponse).toList();
    }

    /**
     * Asocia (o actualiza) un articulo que vende el proveedor. El articulo tiene que existir y estar
     * activo en el catalogo. Si se marca como preferido, deja de serlo el que lo era antes.
     */
    @Transactional
    public ArticuloProveedorResponse asociarArticulo(Long proveedorId, ArticuloProveedorRequest request) {
        Proveedor proveedor = buscar(proveedorId);
        if (!proveedor.isActivo()) {
            throw new ReglaNegocioException("El proveedor " + proveedor.getRazonSocial() + " esta dado de baja");
        }
        ReglasArticuloRemoto articulo = catalogoClient.obtenerReglas(request.getArticuloId())
                .orElseThrow(() -> new ReglaNegocioException("El articulo " + request.getArticuloId() + " no existe en el catalogo"));
        if (!articulo.isActivo()) {
            throw new ReglaNegocioException("El articulo " + articulo.getSku() + " esta dado de baja en el catalogo");
        }

        ArticuloProveedor ap = articuloProveedorRepository.findByProveedorIdAndArticuloId(proveedorId, request.getArticuloId())
                .orElseGet(ArticuloProveedor::new);
        ap.setProveedor(proveedor);
        ap.setArticuloId(articulo.getArticuloId());
        ap.setSku(articulo.getSku());
        ap.setNombreArticulo(articulo.getNombre());
        ap.setPrecioUnitario(request.getPrecioUnitario());
        if (request.isPreferido()) {
            for (ArticuloProveedor otro : articuloProveedorRepository.findByArticuloId(articulo.getArticuloId())) {
                if (otro.isPreferido() && !otro.getProveedor().getId().equals(proveedorId)) {
                    otro.setPreferido(false);
                    articuloProveedorRepository.save(otro);
                }
            }
        }
        ap.setPreferido(request.isPreferido());
        return ComprasMapper.toResponse(articuloProveedorRepository.save(ap));
    }

    @Transactional
    public void desasociarArticulo(Long proveedorId, Long articuloId) {
        ArticuloProveedor ap = articuloProveedorRepository.findByProveedorIdAndArticuloId(proveedorId, articuloId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El proveedor " + proveedorId + " no tiene asociado el articulo " + articuloId));
        articuloProveedorRepository.delete(ap);
    }

    public Proveedor buscar(Long id) {
        return proveedorRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proveedor", id));
    }
}
