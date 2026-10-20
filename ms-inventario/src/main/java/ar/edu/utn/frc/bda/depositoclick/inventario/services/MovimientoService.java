package ar.edu.utn.frc.bda.depositoclick.inventario.services;

import ar.edu.utn.frc.bda.depositoclick.commons.dtos.PaginaResponse;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ConflictoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import ar.edu.utn.frc.bda.depositoclick.commons.security.Roles;
import ar.edu.utn.frc.bda.depositoclick.commons.security.UsuarioActual;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.AjusteRequest;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.EgresoRequest;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.IngresoRequest;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.ItemIngresoDTO;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.ItemSalidaDTO;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.MovimientoResponse;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.OperacionResponse;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.TransferenciaRequest;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.ArticuloSnapshot;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.Deposito;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.Existencia;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.MotivoMovimiento;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.Movimiento;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.OperacionRegistrada;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.TipoMovimiento;
import ar.edu.utn.frc.bda.depositoclick.inventario.repositories.ExistenciaRepository;
import ar.edu.utn.frc.bda.depositoclick.inventario.repositories.MovimientoRepository;
import ar.edu.utn.frc.bda.depositoclick.inventario.repositories.OperacionRegistradaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Logica de negocio de los movimientos de stock. Todas las operaciones son @Transactional:
 * o se aplican TODOS los items, o ninguno (si el tercer item no tiene stock, los dos primeros
 * tampoco se descuentan).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final ExistenciaRepository existenciaRepository;
    private final MovimientoRepository movimientoRepository;
    private final OperacionRegistradaRepository operacionRepository;
    private final DepositoService depositoService;
    private final ReglasArticuloService reglasService;

    // ------------------------------------------------------------------ INGRESOS

    @Transactional
    public OperacionResponse registrarIngreso(IngresoRequest request) {
        MotivoMovimiento motivo = request.getMotivo();
        if (!MotivoMovimiento.DE_INGRESO.contains(motivo)) {
            throw new ReglaNegocioException("El motivo " + motivo + " no corresponde a un ingreso (validos: "
                    + MotivoMovimiento.DE_INGRESO + ")");
        }
        if (motivo == MotivoMovimiento.COMPRA && request.getReferencia() == null) {
            throw new ReglaNegocioException("Un ingreso por COMPRA debe informar la referencia del documento (orden de compra o remito)");
        }
        Optional<OperacionResponse> repetida = buscarRepetida(request.getReferencia(), "INGRESO");
        if (repetida.isPresent()) {
            return repetida.get();
        }

        Deposito deposito = depositoService.buscarActivo(request.getDepositoId());
        OperacionRegistrada operacion = registrarOperacion(request.getReferencia(), "INGRESO", motivo, "ING");
        List<String> advertencias = new ArrayList<>();
        Map<Long, ArticuloSnapshot> reglasPorArticulo = new HashMap<>();
        LocalDate hoy = LocalDate.now();

        List<Movimiento> movimientos = new ArrayList<>();
        for (ItemIngresoDTO item : request.getItems()) {
            ArticuloSnapshot reglas = reglasPorArticulo.computeIfAbsent(item.getArticuloId(),
                    id -> reglasService.obtenerParaOperar(id, advertencias));
            if (!reglas.isActivo()) {
                throw new ReglaNegocioException("El articulo " + reglas.getSku() + " esta dado de baja en el catalogo: no puede recibir stock");
            }
            validarCantidad(reglas, item.getCantidad());
            String lote = resolverLote(reglas, item.getLote());
            LocalDate vencimiento = resolverVencimiento(reglas, item.getFechaVencimiento(), true, hoy);
            Existencia existencia = buscarOCrearExistencia(deposito, reglas, lote, vencimiento);
            movimientos.add(aplicar(existencia, TipoMovimiento.INGRESO, motivo, item.getCantidad(), operacion,
                    request.getObservacion()));
        }
        return armarRespuesta(operacion, false, advertencias, movimientos);
    }

    // ------------------------------------------------------------------ EGRESOS

    @Transactional
    public OperacionResponse registrarEgreso(EgresoRequest request) {
        MotivoMovimiento motivo = request.getMotivo();
        if (!MotivoMovimiento.DE_EGRESO.contains(motivo)) {
            throw new ReglaNegocioException("El motivo " + motivo + " no corresponde a un egreso (validos: "
                    + MotivoMovimiento.DE_EGRESO + ")");
        }
        // El punto de venta solo vende: las bajas por rotura, vencimiento, etc. las hace el deposito
        boolean esSoloVendedor = UsuarioActual.tieneRol(Roles.VENDEDOR)
                && !UsuarioActual.tieneRol(Roles.OPERARIO) && !UsuarioActual.tieneRol(Roles.ADMIN);
        if (esSoloVendedor && motivo != MotivoMovimiento.VENTA) {
            throw new AccessDeniedException("El rol VENDEDOR solo puede registrar egresos por VENTA");
        }
        Optional<OperacionResponse> repetida = buscarRepetida(request.getReferencia(), "EGRESO");
        if (repetida.isPresent()) {
            return repetida.get();
        }

        Deposito deposito = depositoService.buscarActivo(request.getDepositoId());
        OperacionRegistrada operacion = registrarOperacion(request.getReferencia(), "EGRESO", motivo, "EGR");
        List<String> advertencias = new ArrayList<>();
        Map<Long, ArticuloSnapshot> reglasPorArticulo = new HashMap<>();
        LocalDate hoy = LocalDate.now();

        List<Movimiento> movimientos = new ArrayList<>();
        for (ItemSalidaDTO item : request.getItems()) {
            // para sacar stock no hace falta que el articulo este activo (se liquida lo que quedo)
            ArticuloSnapshot reglas = reglasPorArticulo.computeIfAbsent(item.getArticuloId(),
                    id -> reglasService.obtenerParaOperar(id, advertencias));
            validarCantidad(reglas, item.getCantidad());
            movimientos.addAll(retirar(deposito, reglas, item.getCantidad(), item.getLote(), motivo,
                    TipoMovimiento.EGRESO, operacion, request.getObservacion(), hoy));
        }
        return armarRespuesta(operacion, false, advertencias, movimientos);
    }

    // ------------------------------------------------------------------ TRANSFERENCIAS

    @Transactional
    public OperacionResponse registrarTransferencia(TransferenciaRequest request) {
        if (request.getDepositoOrigenId().equals(request.getDepositoDestinoId())) {
            throw new ReglaNegocioException("El deposito de origen y el de destino no pueden ser el mismo");
        }
        Optional<OperacionResponse> repetida = buscarRepetida(request.getReferencia(), "TRANSFERENCIA");
        if (repetida.isPresent()) {
            return repetida.get();
        }

        Deposito origen = depositoService.buscarActivo(request.getDepositoOrigenId());
        Deposito destino = depositoService.buscarActivo(request.getDepositoDestinoId());
        OperacionRegistrada operacion = registrarOperacion(request.getReferencia(), "TRANSFERENCIA",
                MotivoMovimiento.TRANSFERENCIA, "TRF");
        String observacion = request.getObservacion() != null ? request.getObservacion()
                : "Transferencia " + origen.getCodigo() + " -> " + destino.getCodigo();
        List<String> advertencias = new ArrayList<>();
        Map<Long, ArticuloSnapshot> reglasPorArticulo = new HashMap<>();
        LocalDate hoy = LocalDate.now();

        List<Movimiento> movimientos = new ArrayList<>();
        for (ItemSalidaDTO item : request.getItems()) {
            ArticuloSnapshot reglas = reglasPorArticulo.computeIfAbsent(item.getArticuloId(),
                    id -> reglasService.obtenerParaOperar(id, advertencias));
            validarCantidad(reglas, item.getCantidad());
            // sale del origen por FEFO (sin vencidos) y entra al destino con el MISMO lote y vencimiento
            List<Movimiento> salidas = retirar(origen, reglas, item.getCantidad(), item.getLote(),
                    MotivoMovimiento.TRANSFERENCIA, TipoMovimiento.TRANSFERENCIA_SALIDA, operacion, observacion, hoy);
            movimientos.addAll(salidas);
            for (Movimiento salida : salidas) {
                Existencia enDestino = buscarOCrearExistencia(destino, reglas, salida.getLote(), salida.getFechaVencimiento());
                movimientos.add(aplicar(enDestino, TipoMovimiento.TRANSFERENCIA_ENTRADA, MotivoMovimiento.TRANSFERENCIA,
                        salida.getCantidad(), operacion, observacion));
            }
        }
        return armarRespuesta(operacion, false, advertencias, movimientos);
    }

    // ------------------------------------------------------------------ AJUSTES (inventario fisico)

    @Transactional
    public OperacionResponse registrarAjuste(AjusteRequest request) {
        Deposito deposito = depositoService.buscarActivo(request.getDepositoId());
        List<String> advertencias = new ArrayList<>();
        ArticuloSnapshot reglas = reglasService.obtenerParaOperar(request.getArticuloId(), advertencias);
        validarCantidad(reglas, request.getCantidadContada());
        String lote = resolverLote(reglas, request.getLote());

        Existencia existencia = existenciaRepository
                .findByDepositoIdAndArticuloIdAndLote(deposito.getId(), reglas.getArticuloId(), lote).orElse(null);
        BigDecimal actual = existencia == null ? BigDecimal.ZERO : existencia.getCantidad();
        BigDecimal diferencia = request.getCantidadContada().subtract(actual);
        if (diferencia.signum() == 0) {
            throw new ReglaNegocioException("La cantidad contada coincide con el stock del sistema: no hay nada que ajustar");
        }
        if (existencia == null) {
            // aparecio mercaderia de un lote que el sistema no tenia: se da de alta (puede estar vencida)
            LocalDate vencimiento = resolverVencimiento(reglas, request.getFechaVencimiento(), false, LocalDate.now());
            existencia = buscarOCrearExistencia(deposito, reglas, lote, vencimiento);
        }

        OperacionRegistrada operacion = registrarOperacion(null, "AJUSTE", MotivoMovimiento.INVENTARIO_FISICO, "AJU");
        TipoMovimiento tipo = diferencia.signum() > 0 ? TipoMovimiento.AJUSTE_POSITIVO : TipoMovimiento.AJUSTE_NEGATIVO;
        Movimiento movimiento = aplicar(existencia, tipo, MotivoMovimiento.INVENTARIO_FISICO, diferencia.abs(), operacion,
                request.getObservacion());
        return armarRespuesta(operacion, false, advertencias, List.of(movimiento));
    }

    // ------------------------------------------------------------------ CONSULTAS

    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoResponse> buscar(Long depositoId, Long articuloId, TipoMovimiento tipo,
                                                     String referencia, LocalDateTime desde, LocalDateTime hasta,
                                                     Pageable pageable) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        return PaginaResponse.de(movimientoRepository.buscar(depositoId, articuloId, tipo, referencia, desde, hasta, pageable),
                MovimientoService::toResponse);
    }

    @Transactional(readOnly = true)
    public OperacionResponse obtenerOperacion(String referencia) {
        OperacionRegistrada operacion = operacionRepository.findByReferencia(referencia)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operacion", referencia));
        return armarRespuesta(operacion, false, List.of(), movimientoRepository.findByReferenciaOrderByIdAsc(referencia));
    }

    // ------------------------------------------------------------------ REGLAS INTERNAS

    /**
     * Saca stock de un articulo en un deposito.
     * - Si se indica lote: de ese lote.
     * - Si no: FEFO (First Expired, First Out), primero lo que vence antes.
     * - Lo vencido NO se despacha, salvo que el motivo sea VENCIMIENTO (en ese caso, SOLO lo vencido).
     * - Nunca deja stock negativo.
     */
    private List<Movimiento> retirar(Deposito deposito, ArticuloSnapshot reglas, BigDecimal cantidad, String lote,
                                     MotivoMovimiento motivo, TipoMovimiento tipo, OperacionRegistrada operacion,
                                     String observacion, LocalDate hoy) {
        boolean soloVencidos = motivo == MotivoMovimiento.VENCIMIENTO;
        List<Existencia> candidatas;
        BigDecimal vencidoNoDespachable = BigDecimal.ZERO;

        if (lote != null) {
            if (!reglas.isControlaLote()) {
                throw new ReglaNegocioException("El articulo " + reglas.getSku() + " no se controla por lote: no informe lote");
            }
            Existencia e = existenciaRepository
                    .findByDepositoIdAndArticuloIdAndLote(deposito.getId(), reglas.getArticuloId(), lote.toUpperCase())
                    .filter(x -> x.getCantidad().signum() > 0)
                    .orElseThrow(() -> new ReglaNegocioException("No hay stock del lote " + lote + " de "
                            + reglas.getSku() + " en el deposito " + deposito.getCodigo()));
            if (soloVencidos && !e.estaVencida(hoy)) {
                throw new ReglaNegocioException("El lote " + lote + " no esta vencido: no se puede dar de baja por VENCIMIENTO");
            }
            if (!soloVencidos && e.estaVencida(hoy)) {
                throw new ReglaNegocioException("El lote " + lote + " de " + reglas.getSku() + " vencio el "
                        + e.getFechaVencimiento() + ": no se puede despachar");
            }
            candidatas = List.of(e);
        } else {
            List<Existencia> todas = existenciaRepository.buscarConStockOrdenFefo(deposito.getId(), reglas.getArticuloId());
            candidatas = todas.stream().filter(e -> soloVencidos == e.estaVencida(hoy)).toList();
            if (!soloVencidos) {
                vencidoNoDespachable = todas.stream().filter(e -> e.estaVencida(hoy))
                        .map(Existencia::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }

        BigDecimal disponible = candidatas.stream().map(Existencia::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (disponible.compareTo(cantidad) < 0) {
            String mensaje = "Stock insuficiente de " + reglas.getSku() + " en el deposito " + deposito.getCodigo()
                    + ": " + (soloVencidos ? "vencido " : "disponible ") + texto(disponible)
                    + ", solicitado " + texto(cantidad);
            if (vencidoNoDespachable.signum() > 0) {
                mensaje += " (ademas hay " + texto(vencidoNoDespachable) + " vencido que no se puede despachar)";
            }
            throw new ReglaNegocioException(mensaje);
        }

        List<Movimiento> resultado = new ArrayList<>();
        BigDecimal pendiente = cantidad;
        for (Existencia e : candidatas) {
            if (pendiente.signum() == 0) {
                break;
            }
            BigDecimal tomar = e.getCantidad().min(pendiente);
            resultado.add(aplicar(e, tipo, motivo, tomar, operacion, observacion));
            pendiente = pendiente.subtract(tomar);
        }
        return resultado;
    }

    /** Modifica el saldo de la existencia y deja asentado el movimiento. */
    private Movimiento aplicar(Existencia existencia, TipoMovimiento tipo, MotivoMovimiento motivo, BigDecimal cantidad,
                               OperacionRegistrada operacion, String observacion) {
        BigDecimal nuevoSaldo = tipo.suma() ? existencia.getCantidad().add(cantidad) : existencia.getCantidad().subtract(cantidad);
        if (nuevoSaldo.signum() < 0) {
            // no deberia pasar nunca (se controla antes), pero es la ultima barrera
            throw new ReglaNegocioException("La operacion dejaria stock negativo en " + existencia.getSku());
        }
        LocalDateTime ahora = LocalDateTime.now();
        existencia.setCantidad(nuevoSaldo);
        existencia.setFechaUltimoMovimiento(ahora);
        existenciaRepository.save(existencia);

        Movimiento m = new Movimiento();
        m.setTipo(tipo);
        m.setMotivo(motivo);
        m.setDeposito(existencia.getDeposito());
        m.setArticuloId(existencia.getArticuloId());
        m.setSku(existencia.getSku());
        m.setLote(existencia.getLote());
        m.setFechaVencimiento(existencia.getFechaVencimiento());
        m.setCantidad(cantidad);
        m.setSaldoLote(nuevoSaldo);
        m.setFecha(ahora);
        m.setUsuario(operacion.getUsuario());
        m.setReferencia(operacion.getReferencia());
        m.setObservacion(observacion);
        return movimientoRepository.save(m);
    }

    private Existencia buscarOCrearExistencia(Deposito deposito, ArticuloSnapshot reglas, String lote, LocalDate vencimiento) {
        Optional<Existencia> enDeposito = existenciaRepository
                .findByDepositoIdAndArticuloIdAndLote(deposito.getId(), reglas.getArticuloId(), lote);
        if (enDeposito.isPresent()) {
            validarMismoVencimiento(enDeposito.get(), vencimiento);
            return enDeposito.get();
        }
        // el mismo lote en otro deposito tiene que tener el mismo vencimiento
        existenciaRepository.findFirstByArticuloIdAndLote(reglas.getArticuloId(), lote)
                .ifPresent(otra -> validarMismoVencimiento(otra, vencimiento));

        Existencia nueva = new Existencia();
        nueva.setDeposito(deposito);
        nueva.setArticuloId(reglas.getArticuloId());
        nueva.setSku(reglas.getSku());
        nueva.setLote(lote);
        nueva.setFechaVencimiento(vencimiento);
        nueva.setCantidad(BigDecimal.ZERO);
        return existenciaRepository.save(nueva);
    }

    private void validarMismoVencimiento(Existencia existente, LocalDate vencimiento) {
        if (!Existencia.SIN_LOTE.equals(existente.getLote()) && !Objects.equals(existente.getFechaVencimiento(), vencimiento)) {
            throw new ReglaNegocioException("El lote " + existente.getLote() + " de " + existente.getSku()
                    + " ya esta registrado con vencimiento " + existente.getFechaVencimiento()
                    + ": no puede tener otro vencimiento (" + vencimiento + ")");
        }
    }

    private void validarCantidad(ArticuloSnapshot reglas, BigDecimal cantidad) {
        if (!reglas.isPermiteDecimales() && cantidad.stripTrailingZeros().scale() > 0) {
            throw new ReglaNegocioException("El articulo " + reglas.getSku() + " se mide en " + reglas.getUnidadMedida()
                    + ": la cantidad debe ser entera (se informo " + cantidad.toPlainString() + ")");
        }
    }

    private String resolverLote(ArticuloSnapshot reglas, String lote) {
        if (reglas.isControlaLote()) {
            if (lote == null || lote.isBlank()) {
                throw new ReglaNegocioException("El articulo " + reglas.getSku() + " se controla por lote: debe informar el lote");
            }
            if (lote.equalsIgnoreCase(Existencia.SIN_LOTE)) {
                throw new ReglaNegocioException("'" + Existencia.SIN_LOTE + "' no es un lote valido");
            }
            return lote.trim().toUpperCase();
        }
        if (lote != null) {
            throw new ReglaNegocioException("El articulo " + reglas.getSku() + " no se controla por lote: no informe lote");
        }
        return Existencia.SIN_LOTE;
    }

    private LocalDate resolverVencimiento(ArticuloSnapshot reglas, LocalDate vencimiento, boolean exigirFuturo, LocalDate hoy) {
        if (reglas.isControlaVencimiento()) {
            if (vencimiento == null) {
                throw new ReglaNegocioException("El articulo " + reglas.getSku() + " controla vencimiento: debe informar la fecha de vencimiento");
            }
            if (exigirFuturo && !vencimiento.isAfter(hoy)) {
                throw new ReglaNegocioException("No se puede ingresar mercaderia vencida o que vence hoy ("
                        + reglas.getSku() + " vence el " + vencimiento + ")");
            }
            return vencimiento;
        }
        if (vencimiento != null) {
            throw new ReglaNegocioException("El articulo " + reglas.getSku() + " no controla vencimiento: no informe fecha de vencimiento");
        }
        return null;
    }

    /** Idempotencia: si la referencia ya se proceso, se devuelve el resultado anterior sin volver a aplicarlo. */
    private Optional<OperacionResponse> buscarRepetida(String referencia, String tipoOperacion) {
        if (referencia == null) {
            return Optional.empty();
        }
        Optional<OperacionRegistrada> existente = operacionRepository.findByReferencia(referencia);
        if (existente.isEmpty()) {
            return Optional.empty();
        }
        OperacionRegistrada op = existente.get();
        if (!op.getTipoOperacion().equals(tipoOperacion)) {
            throw new ConflictoException("La referencia " + referencia + " ya fue usada por una operacion de tipo " + op.getTipoOperacion());
        }
        log.info("Operacion {} repetida: se devuelve el resultado original", referencia);
        return Optional.of(armarRespuesta(op, true,
                List.of("La operacion ya habia sido registrada el " + op.getFecha() + ": no se volvio a aplicar"),
                movimientoRepository.findByReferenciaOrderByIdAsc(referencia)));
    }

    private OperacionRegistrada registrarOperacion(String referencia, String tipoOperacion, MotivoMovimiento motivo, String prefijo) {
        String ref = referencia != null ? referencia
                : prefijo + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OperacionRegistrada op = OperacionRegistrada.builder()
                .referencia(ref)
                .tipoOperacion(tipoOperacion)
                .motivo(motivo)
                .fecha(LocalDateTime.now())
                .usuario(UsuarioActual.nombre())
                .build();
        try {
            // flush inmediato: si otra peticion con la misma referencia entro al mismo tiempo, falla aca
            return operacionRepository.saveAndFlush(op);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoException("La operacion con referencia " + ref + " se esta procesando en otra peticion");
        }
    }

    private OperacionResponse armarRespuesta(OperacionRegistrada op, boolean repetida, List<String> advertencias,
                                             List<Movimiento> movimientos) {
        return OperacionResponse.builder()
                .referencia(op.getReferencia())
                .tipoOperacion(op.getTipoOperacion())
                .fecha(op.getFecha())
                .usuario(op.getUsuario())
                .repetida(repetida)
                .advertencias(advertencias.isEmpty() ? null : advertencias)
                .movimientos(movimientos.stream().map(MovimientoService::toResponse).toList())
                .build();
    }

    private static String texto(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    public static MovimientoResponse toResponse(Movimiento m) {
        return MovimientoResponse.builder()
                .id(m.getId())
                .tipo(m.getTipo())
                .motivo(m.getMotivo())
                .depositoId(m.getDeposito().getId())
                .depositoCodigo(m.getDeposito().getCodigo())
                .articuloId(m.getArticuloId())
                .sku(m.getSku())
                .lote(m.getLote())
                .fechaVencimiento(m.getFechaVencimiento())
                .cantidad(m.getCantidad())
                .saldoLote(m.getSaldoLote())
                .fecha(m.getFecha())
                .usuario(m.getUsuario())
                .referencia(m.getReferencia())
                .observacion(m.getObservacion())
                .build();
    }
}
