package ar.edu.utn.frc.bda.depositoclick.inventario.services;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.ExistenciaResponse;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.ResumenReposicionDTO;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.StockArticuloResponse;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.ArticuloSnapshot;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.Deposito;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.Existencia;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.MotivoMovimiento;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.TipoMovimiento;
import ar.edu.utn.frc.bda.depositoclick.inventario.repositories.ArticuloSnapshotRepository;
import ar.edu.utn.frc.bda.depositoclick.inventario.repositories.ConsumoArticulo;
import ar.edu.utn.frc.bda.depositoclick.inventario.repositories.ExistenciaRepository;
import ar.edu.utn.frc.bda.depositoclick.inventario.repositories.MovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ExistenciaRepository existenciaRepository;
    private final MovimientoRepository movimientoRepository;
    private final ArticuloSnapshotRepository snapshotRepository;
    private final DepositoService depositoService;
    private final ReglasArticuloService reglasService;

    @Transactional(readOnly = true)
    public List<ExistenciaResponse> consultarExistencias(Long depositoId, Long articuloId) {
        List<Existencia> existencias;
        if (depositoId != null && articuloId != null) {
            existencias = existenciaRepository.findByDepositoIdAndArticuloIdAndCantidadGreaterThan(depositoId, articuloId, BigDecimal.ZERO);
        } else if (depositoId != null) {
            existencias = existenciaRepository.findByDepositoIdAndCantidadGreaterThanOrderByArticuloIdAsc(depositoId, BigDecimal.ZERO);
        } else if (articuloId != null) {
            existencias = existenciaRepository.findByArticuloIdAndCantidadGreaterThanOrderByDepositoIdAsc(articuloId, BigDecimal.ZERO);
        } else {
            existencias = existenciaRepository.findByCantidadGreaterThanOrderByArticuloIdAsc(BigDecimal.ZERO);
        }
        LocalDate hoy = LocalDate.now();
        return existencias.stream().map(e -> toResponse(e, hoy)).toList();
    }

    /** Stock consolidado de un articulo (lo usa el PoS para saber si puede vender). */
    @Transactional
    public StockArticuloResponse stockDeArticulo(Long articuloId, Long depositoId) {
        ArticuloSnapshot reglas = reglasService.obtenerParaConsultar(articuloId);
        List<Existencia> existencias = depositoId == null
                ? existenciaRepository.findByArticuloIdAndCantidadGreaterThanOrderByDepositoIdAsc(articuloId, BigDecimal.ZERO)
                : existenciaRepository.findByDepositoIdAndArticuloIdAndCantidadGreaterThan(depositoId, articuloId, BigDecimal.ZERO);
        return armarStock(reglas, existencias, true);
    }

    /** Articulos cuyo stock disponible (sin vencidos) esta por debajo del stock minimo del catalogo. */
    @Transactional
    public List<StockArticuloResponse> bajoMinimo(Long depositoId) {
        if (depositoId != null) {
            depositoService.buscar(depositoId);
        }
        // universo: todos los articulos que alguna vez tuvieron movimiento en inventario
        List<Long> ids = snapshotRepository.findAll().stream().map(ArticuloSnapshot::getArticuloId).toList();
        Map<Long, ArticuloSnapshot> reglas = reglasService.obtenerVarios(ids);

        List<Existencia> existencias = depositoId == null
                ? existenciaRepository.findByCantidadGreaterThanOrderByArticuloIdAsc(BigDecimal.ZERO)
                : existenciaRepository.findByDepositoIdAndCantidadGreaterThanOrderByArticuloIdAsc(depositoId, BigDecimal.ZERO);
        Map<Long, List<Existencia>> porArticulo = existencias.stream().collect(Collectors.groupingBy(Existencia::getArticuloId));

        List<StockArticuloResponse> resultado = new ArrayList<>();
        for (ArticuloSnapshot r : reglas.values()) {
            if (!r.isActivo()) {
                continue; // un articulo dado de baja no "falta"
            }
            StockArticuloResponse stock = armarStock(r, porArticulo.getOrDefault(r.getArticuloId(), List.of()), false);
            if (stock.isBajoMinimo()) {
                resultado.add(stock);
            }
        }
        resultado.sort((a, b) -> a.getSku().compareTo(b.getSku()));
        return resultado;
    }

    /** Lotes que vencen dentro de los proximos N dias (incluye los ya vencidos que siguen en stock). */
    @Transactional(readOnly = true)
    public List<ExistenciaResponse> proximosVencimientos(int dias) {
        if (dias < 0 || dias > 365) {
            throw new ReglaNegocioException("Los dias deben estar entre 0 y 365");
        }
        LocalDate hoy = LocalDate.now();
        return existenciaRepository.buscarQueVencenAntesDe(hoy.plusDays(dias)).stream()
                .map(e -> toResponse(e, hoy)).toList();
    }

    /**
     * Informacion para la reposicion de un deposito (la consume ms-compras):
     * por articulo, stock disponible y consumo promedio diario de los ultimos N dias.
     */
    @Transactional(readOnly = true)
    public List<ResumenReposicionDTO> resumenReposicion(Long depositoId, int dias) {
        if (dias < 7 || dias > 180) {
            throw new ReglaNegocioException("El periodo de consumo debe estar entre 7 y 180 dias");
        }
        Deposito deposito = depositoService.buscar(depositoId);
        LocalDate hoy = LocalDate.now();

        Map<Long, BigDecimal> disponible = new HashMap<>();
        Map<Long, String> skus = new HashMap<>();
        for (Existencia e : existenciaRepository.findByDepositoIdAndCantidadGreaterThanOrderByArticuloIdAsc(deposito.getId(), BigDecimal.ZERO)) {
            skus.put(e.getArticuloId(), e.getSku());
            if (!e.estaVencida(hoy)) {
                disponible.merge(e.getArticuloId(), e.getCantidad(), BigDecimal::add);
            }
        }
        Map<Long, BigDecimal> consumo = new HashMap<>();
        List<ConsumoArticulo> consumos = movimientoRepository.consumoPorArticulo(deposito.getId(), TipoMovimiento.EGRESO,
                MotivoMovimiento.DE_CONSUMO, hoy.minusDays(dias).atStartOfDay());
        for (ConsumoArticulo c : consumos) {
            consumo.put(c.getArticuloId(), c.getTotal());
        }

        Set<Long> articulos = new TreeSet<>(skus.keySet());
        articulos.addAll(consumo.keySet());
        if (articulos.stream().anyMatch(id -> !skus.containsKey(id))) {
            snapshotRepository.findAllById(articulos).forEach(s -> skus.putIfAbsent(s.getArticuloId(), s.getSku()));
        }

        List<ResumenReposicionDTO> resultado = new ArrayList<>();
        for (Long id : articulos) {
            BigDecimal consumido = consumo.getOrDefault(id, BigDecimal.ZERO);
            resultado.add(ResumenReposicionDTO.builder()
                    .articuloId(id)
                    .sku(skus.get(id))
                    .stockDisponible(disponible.getOrDefault(id, BigDecimal.ZERO))
                    .consumoPeriodo(consumido)
                    .diasPeriodo(dias)
                    .consumoDiarioPromedio(consumido.divide(BigDecimal.valueOf(dias), 3, RoundingMode.HALF_UP))
                    .build());
        }
        return resultado;
    }

    private StockArticuloResponse armarStock(ArticuloSnapshot reglas, List<Existencia> existencias, boolean conDetalle) {
        LocalDate hoy = LocalDate.now();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;
        for (Existencia e : existencias) {
            total = total.add(e.getCantidad());
            if (e.estaVencida(hoy)) {
                vencido = vencido.add(e.getCantidad());
            }
        }
        BigDecimal disponible = total.subtract(vencido);
        return StockArticuloResponse.builder()
                .articuloId(reglas.getArticuloId())
                .sku(reglas.getSku())
                .nombre(reglas.getNombre())
                .unidadMedida(reglas.getUnidadMedida())
                .stockTotal(total)
                .stockDisponible(disponible)
                .stockVencido(vencido)
                .stockMinimo(reglas.getStockMinimo())
                .bajoMinimo(disponible.compareTo(reglas.getStockMinimo()) < 0)
                .existencias(conDetalle ? existencias.stream().map(e -> toResponse(e, hoy)).toList() : null)
                .build();
    }

    private ExistenciaResponse toResponse(Existencia e, LocalDate hoy) {
        return ExistenciaResponse.builder()
                .depositoId(e.getDeposito().getId())
                .depositoCodigo(e.getDeposito().getCodigo())
                .articuloId(e.getArticuloId())
                .sku(e.getSku())
                .lote(e.getLote())
                .fechaVencimiento(e.getFechaVencimiento())
                .cantidad(e.getCantidad())
                .vencida(e.estaVencida(hoy))
                .diasParaVencer(e.getFechaVencimiento() == null ? null : ChronoUnit.DAYS.between(hoy, e.getFechaVencimiento()))
                .build();
    }
}
