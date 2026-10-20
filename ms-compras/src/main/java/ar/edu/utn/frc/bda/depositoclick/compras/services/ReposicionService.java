package ar.edu.utn.frc.bda.depositoclick.compras.services;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import ar.edu.utn.frc.bda.depositoclick.commons.security.UsuarioActual;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.CatalogoClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.InventarioClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.DepositoRemoto;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.ReglasArticuloRemoto;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.ResumenReposicionRemoto;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.CalculoEntregaResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.OrdenResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.SugerenciasResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.SugerenciasResponse.Sugerencia;
import ar.edu.utn.frc.bda.depositoclick.compras.models.ArticuloProveedor;
import ar.edu.utn.frc.bda.depositoclick.compras.models.DetalleOrden;
import ar.edu.utn.frc.bda.depositoclick.compras.models.EstadoOrden;
import ar.edu.utn.frc.bda.depositoclick.compras.models.OrdenCompra;
import ar.edu.utn.frc.bda.depositoclick.compras.models.Proveedor;
import ar.edu.utn.frc.bda.depositoclick.compras.repositories.ArticuloProveedorRepository;
import ar.edu.utn.frc.bda.depositoclick.compras.repositories.OrdenCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Calculo de reposicion (el proceso mas "colaborativo" de la solucion):
 *
 *   ms-inventario -> stock disponible y consumo promedio diario del deposito
 *   ms-catalogo   -> stock minimo, unidad y si el articulo sigue activo
 *   propio        -> proveedor preferido, lead time, precio y lo ya pedido (ordenes en camino)
 *   API externa   -> feriados del pais del deposito, para pasar el lead time de dias HABILES a
 *                    dias CORRIDOS (el consumo ocurre todos los dias, el proveedor entrega solo en habiles)
 *
 * Formulas (por articulo):
 *   diasEspera     = dias corridos desde hoy hasta la fecha de entrega (hoy + leadTime habiles)
 *   puntoDePedido  = consumoDiario * diasEspera + stockMinimo
 *   posicion       = stockDisponible + pendienteEnOrdenes
 *   si posicion < puntoDePedido  ->  sugerido = consumoDiario * (diasEspera + diasCobertura) + stockMinimo - posicion
 *   (redondeado hacia arriba; a entero si la unidad no admite decimales)
 */
@Service
@RequiredArgsConstructor
public class ReposicionService {

    private static final EnumSet<EstadoOrden> EN_CAMINO = EnumSet.of(EstadoOrden.EMITIDA, EstadoOrden.RECIBIDA_PARCIAL);

    private final InventarioClient inventarioClient;
    private final CatalogoClient catalogoClient;
    private final ArticuloProveedorRepository articuloProveedorRepository;
    private final OrdenCompraRepository ordenRepository;
    private final CalendarioLaboralService calendarioService;
    private final OrdenCompraService ordenCompraService;

    @Transactional(readOnly = true)
    public SugerenciasResponse calcularSugerencias(Long depositoId, int diasCobertura, int diasHistoria) {
        if (diasCobertura < 1 || diasCobertura > 120) {
            throw new ReglaNegocioException("Los dias de cobertura deben estar entre 1 y 120");
        }
        DepositoRemoto deposito = ordenCompraService.buscarDepositoActivo(depositoId);
        LocalDate hoy = LocalDate.now();

        // 1) stock y consumo (ms-inventario)
        Map<Long, ResumenReposicionRemoto> resumen = new HashMap<>();
        for (ResumenReposicionRemoto r : inventarioClient.resumenReposicion(depositoId, diasHistoria)) {
            resumen.put(r.getArticuloId(), r);
        }

        // 2) proveedor elegido por articulo (preferido; si no hay, el mas barato)
        Map<Long, ArticuloProveedor> proveedorPorArticulo = new HashMap<>();
        for (ArticuloProveedor ap : articuloProveedorRepository.buscarDeProveedoresActivos()) {
            ArticuloProveedor actual = proveedorPorArticulo.get(ap.getArticuloId());
            if (actual == null || (ap.isPreferido() && !actual.isPreferido())
                    || (ap.isPreferido() == actual.isPreferido() && ap.getPrecioUnitario().compareTo(actual.getPrecioUnitario()) < 0)) {
                proveedorPorArticulo.put(ap.getArticuloId(), ap);
            }
        }

        // 3) lo que ya esta pedido y no llego
        Map<Long, BigDecimal> pendientes = new HashMap<>();
        for (OrdenCompra o : ordenRepository.buscarPorDepositoYEstados(depositoId, EN_CAMINO)) {
            for (DetalleOrden d : o.getDetalles()) {
                pendientes.merge(d.getArticuloId(), d.getPendiente(), BigDecimal::add);
            }
        }

        // 4) reglas del catalogo para todos los articulos involucrados (una sola llamada)
        Set<Long> ids = new TreeSet<>(resumen.keySet());
        ids.addAll(proveedorPorArticulo.keySet());
        Map<Long, ReglasArticuloRemoto> reglas = new HashMap<>();
        for (ReglasArticuloRemoto r : catalogoClient.obtenerReglas(ids)) {
            reglas.put(r.getArticuloId(), r);
        }

        // 5) calculo
        Map<Integer, CalculoEntregaResponse> entregaPorLeadTime = new HashMap<>();
        Set<String> advertencias = new LinkedHashSet<>();
        boolean calendarioConfiable = true;
        List<Sugerencia> sugerencias = new ArrayList<>();

        for (Long articuloId : ids) {
            ReglasArticuloRemoto articulo = reglas.get(articuloId);
            if (articulo == null || !articulo.isActivo()) {
                continue; // dado de baja o borrado: no se repone
            }
            ResumenReposicionRemoto r = resumen.get(articuloId);
            BigDecimal stock = r == null ? BigDecimal.ZERO : r.getStockDisponible();
            BigDecimal consumoDiario = r == null || r.getConsumoDiarioPromedio() == null ? BigDecimal.ZERO : r.getConsumoDiarioPromedio();
            BigDecimal pendiente = pendientes.getOrDefault(articuloId, BigDecimal.ZERO);
            BigDecimal posicion = stock.add(pendiente);
            ArticuloProveedor ap = proveedorPorArticulo.get(articuloId);

            Sugerencia.SugerenciaBuilder s = Sugerencia.builder()
                    .articuloId(articuloId)
                    .sku(articulo.getSku())
                    .nombre(articulo.getNombre())
                    .unidadMedida(articulo.getUnidadMedida())
                    .stockDisponible(stock)
                    .pendienteEnOrdenes(pendiente)
                    .consumoDiarioPromedio(consumoDiario)
                    .stockMinimo(articulo.getStockMinimo());

            if (ap == null) {
                // sin proveedor no hay lead time: solo avisamos si esta por debajo del minimo
                if (posicion.compareTo(articulo.getStockMinimo()) < 0) {
                    sugerencias.add(s.puntoDePedido(articulo.getStockMinimo())
                            .cantidadSugerida(redondear(articulo.getStockMinimo().subtract(posicion), articulo))
                            .explicacion("Esta por debajo del stock minimo pero NO tiene proveedor asociado: asocie uno para poder pedirlo")
                            .build());
                }
                continue;
            }

            Proveedor proveedor = ap.getProveedor();
            CalculoEntregaResponse entrega = entregaPorLeadTime.computeIfAbsent(proveedor.getLeadTimeDiasHabiles(),
                    lt -> calendarioService.sumarDiasHabiles(deposito.getPais(), hoy, lt));
            calendarioConfiable &= entrega.isConfiable();
            if (entrega.getAdvertencias() != null) {
                advertencias.addAll(entrega.getAdvertencias());
            }
            BigDecimal diasEspera = BigDecimal.valueOf(entrega.getDiasCorridos());
            BigDecimal puntoDePedido = consumoDiario.multiply(diasEspera).add(articulo.getStockMinimo());

            if (posicion.compareTo(puntoDePedido) >= 0) {
                continue; // alcanza hasta que llegue un pedido nuevo
            }
            BigDecimal objetivo = consumoDiario.multiply(diasEspera.add(BigDecimal.valueOf(diasCobertura))).add(articulo.getStockMinimo());
            BigDecimal sugerido = redondear(objetivo.subtract(posicion), articulo);
            if (sugerido.signum() <= 0) {
                continue;
            }
            sugerencias.add(s
                    .proveedorId(proveedor.getId())
                    .proveedorRazonSocial(proveedor.getRazonSocial())
                    .leadTimeDiasHabiles(proveedor.getLeadTimeDiasHabiles())
                    .diasCorridosHastaEntrega(entrega.getDiasCorridos())
                    .fechaEstimadaEntrega(entrega.getFechaEntrega())
                    .puntoDePedido(puntoDePedido.setScale(3, RoundingMode.HALF_UP))
                    .cantidadSugerida(sugerido)
                    .precioUnitario(ap.getPrecioUnitario())
                    .explicacion("Disponible + en camino (" + texto(posicion) + ") por debajo del punto de pedido ("
                            + texto(puntoDePedido) + "): el proveedor tarda " + proveedor.getLeadTimeDiasHabiles()
                            + " dias habiles = " + entrega.getDiasCorridos() + " dias corridos"
                            + (entrega.getFeriadosSalteados().isEmpty() ? "" : " (incluye feriados: " + String.join(", ", entrega.getFeriadosSalteados()) + ")"))
                    .build());
        }
        sugerencias.sort(Comparator.comparing(Sugerencia::getSku));

        return SugerenciasResponse.builder()
                .depositoId(deposito.getId())
                .depositoCodigo(deposito.getCodigo())
                .pais(deposito.getPais())
                .fechaCalculo(hoy)
                .diasCobertura(diasCobertura)
                .diasHistoriaConsumo(diasHistoria)
                .calendarioConfiable(calendarioConfiable)
                .advertencias(advertencias.isEmpty() ? null : new ArrayList<>(advertencias))
                .sugerencias(sugerencias)
                .build();
    }

    /** Convierte las sugerencias en ordenes BORRADOR, una por proveedor (despues alguien las revisa y emite). */
    @Transactional
    public List<OrdenResponse> generarBorradores(Long depositoId, int diasCobertura, int diasHistoria) {
        SugerenciasResponse calculo = calcularSugerencias(depositoId, diasCobertura, diasHistoria);
        Map<Long, List<Sugerencia>> porProveedor = new LinkedHashMap<>();
        for (Sugerencia s : calculo.getSugerencias()) {
            if (s.getProveedorId() != null) {
                porProveedor.computeIfAbsent(s.getProveedorId(), k -> new ArrayList<>()).add(s);
            }
        }
        if (porProveedor.isEmpty()) {
            throw new ReglaNegocioException("No hay articulos para reponer en el deposito " + calculo.getDepositoCodigo());
        }

        List<OrdenResponse> creadas = new ArrayList<>();
        for (Map.Entry<Long, List<Sugerencia>> entry : porProveedor.entrySet()) {
            OrdenCompra orden = new OrdenCompra();
            List<Sugerencia> items = entry.getValue();
            Proveedor proveedor = articuloProveedorRepository
                    .findByProveedorIdAndArticuloId(entry.getKey(), items.get(0).getArticuloId())
                    .orElseThrow().getProveedor();
            orden.setProveedor(proveedor);
            orden.setDepositoDestinoId(calculo.getDepositoId());
            orden.setDepositoCodigo(calculo.getDepositoCodigo());
            orden.setPaisDeposito(calculo.getPais());
            orden.setEstado(EstadoOrden.BORRADOR);
            orden.setFechaCreacion(LocalDateTime.now());
            orden.setCreadoPor(UsuarioActual.nombre());
            orden.setObservacion("Generada por reposicion automatica (" + diasCobertura + " dias de cobertura)");
            for (Sugerencia s : items) {
                DetalleOrden d = new DetalleOrden();
                d.setArticuloId(s.getArticuloId());
                d.setSku(s.getSku());
                d.setNombreArticulo(s.getNombre());
                d.setUnidadMedida(s.getUnidadMedida());
                d.setCantidadPedida(s.getCantidadSugerida());
                d.setCantidadRecibida(BigDecimal.ZERO);
                d.setPrecioUnitario(s.getPrecioUnitario());
                orden.agregarDetalle(d);
            }
            creadas.add(ComprasMapper.toResponse(ordenRepository.save(orden)));
        }
        return creadas;
    }

    private BigDecimal redondear(BigDecimal valor, ReglasArticuloRemoto articulo) {
        if (valor.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return articulo.isPermiteDecimales() ? valor.setScale(3, RoundingMode.CEILING) : valor.setScale(0, RoundingMode.CEILING);
    }

    private static String texto(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
