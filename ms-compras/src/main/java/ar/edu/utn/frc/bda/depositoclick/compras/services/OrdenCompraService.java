package ar.edu.utn.frc.bda.depositoclick.compras.services;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ConflictoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import ar.edu.utn.frc.bda.depositoclick.commons.security.UsuarioActual;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.CatalogoClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.InventarioClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.DepositoRemoto;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.IngresoRemotoRequest;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.OperacionRemota;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.ReglasArticuloRemoto;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.CalculoEntregaResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.OrdenRequest;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.OrdenResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.RecepcionRequest;
import ar.edu.utn.frc.bda.depositoclick.compras.models.ArticuloProveedor;
import ar.edu.utn.frc.bda.depositoclick.compras.models.DetalleOrden;
import ar.edu.utn.frc.bda.depositoclick.compras.models.EstadoOrden;
import ar.edu.utn.frc.bda.depositoclick.compras.models.OrdenCompra;
import ar.edu.utn.frc.bda.depositoclick.compras.models.Proveedor;
import ar.edu.utn.frc.bda.depositoclick.compras.models.Recepcion;
import ar.edu.utn.frc.bda.depositoclick.compras.models.RecepcionItem;
import ar.edu.utn.frc.bda.depositoclick.compras.repositories.ArticuloProveedorRepository;
import ar.edu.utn.frc.bda.depositoclick.compras.repositories.OrdenCompraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private final OrdenCompraRepository ordenRepository;
    private final ArticuloProveedorRepository articuloProveedorRepository;
    private final ProveedorService proveedorService;
    private final CatalogoClient catalogoClient;
    private final InventarioClient inventarioClient;
    private final CalendarioLaboralService calendarioService;

    @Transactional(readOnly = true)
    public List<OrdenResponse> listar(EstadoOrden estado, Long proveedorId) {
        List<OrdenCompra> ordenes;
        if (estado != null && proveedorId != null) {
            ordenes = ordenRepository.findByProveedorIdAndEstadoOrderByIdDesc(proveedorId, estado);
        } else if (estado != null) {
            ordenes = ordenRepository.findByEstadoOrderByIdDesc(estado);
        } else if (proveedorId != null) {
            ordenes = ordenRepository.findByProveedorIdOrderByIdDesc(proveedorId);
        } else {
            ordenes = ordenRepository.findAllByOrderByIdDesc();
        }
        return ordenes.stream().map(ComprasMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrdenResponse obtener(Long id) {
        return ComprasMapper.toResponse(buscar(id));
    }

    /**
     * Crea una orden en BORRADOR. Colaboracion:
     *  - ms-inventario: el deposito destino existe y esta activo (y de que pais es)
     *  - ms-catalogo:   cada articulo existe, esta activo y en que unidad se mide
     *  - propio:        el proveedor esta activo y vende esos articulos (precio pactado)
     */
    @Transactional
    public OrdenResponse crear(OrdenRequest request) {
        Proveedor proveedor = proveedorService.buscar(request.getProveedorId());
        if (!proveedor.isActivo()) {
            throw new ReglaNegocioException("El proveedor " + proveedor.getRazonSocial() + " esta dado de baja");
        }
        DepositoRemoto deposito = buscarDepositoActivo(request.getDepositoDestinoId());

        OrdenCompra orden = new OrdenCompra();
        orden.setProveedor(proveedor);
        orden.setDepositoDestinoId(deposito.getId());
        orden.setDepositoCodigo(deposito.getCodigo());
        orden.setPaisDeposito(deposito.getPais());
        orden.setEstado(EstadoOrden.BORRADOR);
        orden.setFechaCreacion(LocalDateTime.now());
        orden.setFechaEntregaSolicitada(request.getFechaEntregaSolicitada());
        orden.setObservacion(request.getObservacion());
        orden.setCreadoPor(UsuarioActual.nombre());

        Set<Long> yaIncluidos = new HashSet<>();
        for (OrdenRequest.Item item : request.getItems()) {
            if (!yaIncluidos.add(item.getArticuloId())) {
                throw new ReglaNegocioException("El articulo " + item.getArticuloId() + " esta repetido en la orden: unifique las cantidades");
            }
            orden.agregarDetalle(armarDetalle(proveedor, item.getArticuloId(), item.getCantidad(), item.getPrecioUnitario()));
        }
        return ComprasMapper.toResponse(ordenRepository.save(orden));
    }

    /**
     * Emite la orden (se le envia al proveedor). Aca se usa la API externa de feriados:
     * fecha estimada = hoy + lead time del proveedor en DIAS HABILES del pais del deposito.
     * Si se pidio una fecha de entrega, tiene que ser un dia habil y alcanzable por el proveedor.
     */
    @Transactional
    public OrdenResponse emitir(Long id) {
        OrdenCompra orden = buscar(id);
        if (orden.getEstado() != EstadoOrden.BORRADOR) {
            throw new ConflictoException("Solo se puede emitir una orden en BORRADOR (estado actual: " + orden.getEstado() + ")");
        }
        Proveedor proveedor = orden.getProveedor();
        if (!proveedor.isActivo()) {
            throw new ReglaNegocioException("El proveedor " + proveedor.getRazonSocial() + " fue dado de baja: no se puede emitir");
        }

        LocalDate hoy = LocalDate.now();
        CalculoEntregaResponse calculo = calendarioService.sumarDiasHabiles(orden.getPaisDeposito(), hoy,
                proveedor.getLeadTimeDiasHabiles());
        List<String> advertencias = calculo.getAdvertencias() == null ? new ArrayList<>() : new ArrayList<>(calculo.getAdvertencias());

        LocalDate solicitada = orden.getFechaEntregaSolicitada();
        if (solicitada != null) {
            String noHabil = calendarioService.motivoNoHabil(orden.getPaisDeposito(), solicitada);
            if (noHabil != null) {
                throw new ReglaNegocioException("La fecha de entrega solicitada " + solicitada + " no es un dia habil en "
                        + orden.getPaisDeposito() + ": " + noHabil);
            }
            if (solicitada.isBefore(calculo.getFechaEntrega())) {
                throw new ReglaNegocioException("El proveedor " + proveedor.getRazonSocial() + " necesita "
                        + proveedor.getLeadTimeDiasHabiles() + " dias habiles: la entrega mas temprana posible es el "
                        + calculo.getFechaEntrega() + " (se pidio para el " + solicitada + ")");
            }
        }

        orden.setEstado(EstadoOrden.EMITIDA);
        orden.setFechaEmision(LocalDateTime.now());
        orden.setFechaEntregaEstimada(solicitada != null ? solicitada : calculo.getFechaEntrega());
        orden.setFuenteCalendario(calculo.getFuente());
        orden.setCalendarioConfiable(calculo.isConfiable());
        OrdenResponse respuesta = ComprasMapper.toResponse(ordenRepository.save(orden));
        respuesta.setFeriadosConsiderados(calculo.getFeriadosSalteados());
        respuesta.setAdvertencias(advertencias.isEmpty() ? null : advertencias);
        return respuesta;
    }

    /**
     * Registra la recepcion (total o parcial) de mercaderia. PROCESO QUE ATRAVIESA SERVICIOS:
     *  1. Compras valida la orden: estado y que no se reciba mas de lo pendiente.
     *  2. Compras le pide a Inventario que registre el ingreso (motivo COMPRA) con la referencia
     *     "OC-{id}-REC-{n}". Inventario valida lote/vencimiento/unidad contra el Catalogo.
     *  3. Solo si Inventario confirma, Compras guarda la recepcion y actualiza la orden.
     *
     * Si Inventario no responde: no se guarda nada y se devuelve 503 (la orden queda igual).
     * Si respondio pero la respuesta se perdio: al reintentar, la referencia es la misma
     * (n no avanzo) e Inventario devuelve el ingreso original sin duplicar stock.
     */
    @Transactional
    public OrdenResponse recibir(Long id, RecepcionRequest request) {
        OrdenCompra orden = buscar(id);
        if (!orden.getEstado().admiteRecepcion()) {
            throw new ConflictoException("La orden " + id + " no admite recepciones (estado: " + orden.getEstado() + ")");
        }

        Map<Long, DetalleOrden> detallePorArticulo = new HashMap<>();
        orden.getDetalles().forEach(d -> detallePorArticulo.put(d.getArticuloId(), d));
        Map<Long, BigDecimal> recibidoAhora = new HashMap<>();
        for (RecepcionRequest.Item item : request.getItems()) {
            DetalleOrden detalle = detallePorArticulo.get(item.getArticuloId());
            if (detalle == null) {
                throw new ReglaNegocioException("El articulo " + item.getArticuloId() + " no forma parte de la orden " + id);
            }
            BigDecimal total = recibidoAhora.merge(item.getArticuloId(), item.getCantidad(), BigDecimal::add);
            if (total.compareTo(detalle.getPendiente()) > 0) {
                throw new ReglaNegocioException("Se intenta recibir " + total.stripTrailingZeros().toPlainString() + " de "
                        + detalle.getSku() + " pero quedan pendientes " + detalle.getPendiente().stripTrailingZeros().toPlainString());
            }
        }

        int numero = orden.getRecepciones().size() + 1;
        String referencia = "OC-" + orden.getId() + "-REC-" + numero;

        List<IngresoRemotoRequest.Item> itemsIngreso = request.getItems().stream()
                .map(i -> new IngresoRemotoRequest.Item(i.getArticuloId(), i.getCantidad(), i.getLote(), i.getFechaVencimiento()))
                .toList();
        OperacionRemota operacion = inventarioClient.registrarIngreso(new IngresoRemotoRequest(
                orden.getDepositoDestinoId(), "COMPRA", referencia,
                "Recepcion " + numero + " de la OC " + orden.getId() + " (" + orden.getProveedor().getRazonSocial() + ")",
                itemsIngreso));
        if (operacion != null && operacion.isRepetida()) {
            log.info("Inventario ya tenia registrado el ingreso {}: se completa la recepcion sin duplicar stock", referencia);
        }

        Recepcion recepcion = new Recepcion();
        recepcion.setNumero(numero);
        recepcion.setFecha(LocalDateTime.now());
        recepcion.setUsuario(UsuarioActual.nombre());
        recepcion.setReferenciaInventario(referencia);
        recepcion.setObservacion(request.getObservacion());
        for (RecepcionRequest.Item i : request.getItems()) {
            RecepcionItem ri = new RecepcionItem();
            ri.setArticuloId(i.getArticuloId());
            ri.setCantidad(i.getCantidad());
            ri.setLote(i.getLote());
            ri.setFechaVencimiento(i.getFechaVencimiento());
            recepcion.agregarItem(ri);
        }
        orden.agregarRecepcion(recepcion);
        recibidoAhora.forEach((articuloId, cantidad) -> {
            DetalleOrden d = detallePorArticulo.get(articuloId);
            d.setCantidadRecibida(d.getCantidadRecibida().add(cantidad));
        });
        orden.setEstado(orden.estaCompleta() ? EstadoOrden.RECIBIDA : EstadoOrden.RECIBIDA_PARCIAL);

        OrdenResponse respuesta = ComprasMapper.toResponse(ordenRepository.save(orden));
        if (operacion != null && operacion.getAdvertencias() != null) {
            respuesta.setAdvertencias(operacion.getAdvertencias());
        }
        return respuesta;
    }

    /** Solo se cancela si no se recibio nada (si ya entro mercaderia, la orden ya tuvo efecto). */
    @Transactional
    public OrdenResponse cancelar(Long id, String motivo) {
        OrdenCompra orden = buscar(id);
        if (orden.getEstado() != EstadoOrden.BORRADOR && orden.getEstado() != EstadoOrden.EMITIDA) {
            throw new ConflictoException("No se puede cancelar una orden en estado " + orden.getEstado()
                    + " (solo BORRADOR o EMITIDA sin recepciones)");
        }
        orden.setEstado(EstadoOrden.CANCELADA);
        orden.setFechaCancelacion(LocalDateTime.now());
        orden.setMotivoCancelacion(motivo);
        return ComprasMapper.toResponse(ordenRepository.save(orden));
    }

    /** Arma un renglon de la orden validando el articulo contra el catalogo y contra el proveedor. */
    DetalleOrden armarDetalle(Proveedor proveedor, Long articuloId, BigDecimal cantidad, BigDecimal precioInformado) {
        ReglasArticuloRemoto articulo = catalogoClient.obtenerReglas(articuloId)
                .orElseThrow(() -> new ReglaNegocioException("El articulo " + articuloId + " no existe en el catalogo"));
        if (!articulo.isActivo()) {
            throw new ReglaNegocioException("El articulo " + articulo.getSku() + " esta dado de baja: no se puede comprar");
        }
        if (!articulo.isPermiteDecimales() && cantidad.stripTrailingZeros().scale() > 0) {
            throw new ReglaNegocioException("El articulo " + articulo.getSku() + " se mide en " + articulo.getUnidadMedida()
                    + ": la cantidad debe ser entera");
        }
        ArticuloProveedor pactado = articuloProveedorRepository.findByProveedorIdAndArticuloId(proveedor.getId(), articuloId)
                .orElseThrow(() -> new ReglaNegocioException("El proveedor " + proveedor.getRazonSocial()
                        + " no tiene asociado el articulo " + articulo.getSku()));

        DetalleOrden d = new DetalleOrden();
        d.setArticuloId(articuloId);
        d.setSku(articulo.getSku());
        d.setNombreArticulo(articulo.getNombre());
        d.setUnidadMedida(articulo.getUnidadMedida());
        d.setCantidadPedida(cantidad);
        d.setCantidadRecibida(BigDecimal.ZERO);
        d.setPrecioUnitario(precioInformado != null ? precioInformado : pactado.getPrecioUnitario());
        return d;
    }

    DepositoRemoto buscarDepositoActivo(Long depositoId) {
        DepositoRemoto deposito = inventarioClient.obtenerDeposito(depositoId)
                .orElseThrow(() -> new ReglaNegocioException("El deposito " + depositoId + " no existe"));
        if (!deposito.isActivo()) {
            throw new ReglaNegocioException("El deposito " + deposito.getCodigo() + " esta dado de baja");
        }
        return deposito;
    }

    OrdenCompra buscar(Long id) {
        return ordenRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Orden de compra", id));
    }
}
