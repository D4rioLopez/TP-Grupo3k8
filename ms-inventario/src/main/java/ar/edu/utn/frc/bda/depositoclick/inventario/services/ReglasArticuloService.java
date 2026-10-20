package ar.edu.utn.frc.bda.depositoclick.inventario.services;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ErrorRemotoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frc.bda.depositoclick.inventario.clients.CatalogoClient;
import ar.edu.utn.frc.bda.depositoclick.inventario.clients.ReglasArticuloRemoto;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.ArticuloSnapshot;
import ar.edu.utn.frc.bda.depositoclick.inventario.repositories.ArticuloSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Obtiene las reglas de stock de un articulo (unidad, lote, vencimiento, minimo, activo).
 *
 * Estrategia ante fallas del catalogo (modo degradado):
 *  1) se le pregunta SIEMPRE al catalogo (es el dueño del dato) y se actualiza la copia local;
 *  2) si el catalogo no responde, se usa la copia local SOLO si es reciente
 *     (app.catalogo.snapshot-validez-horas), y se avisa en la respuesta;
 *  3) si no hay copia reciente, la operacion se rechaza con 503 (preferimos no mover stock
 *     con reglas que no podemos verificar).
 */
@Slf4j
@Service
public class ReglasArticuloService {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CatalogoClient catalogoClient;
    private final ArticuloSnapshotRepository snapshotRepository;
    private final long validezHoras;

    public ReglasArticuloService(CatalogoClient catalogoClient, ArticuloSnapshotRepository snapshotRepository,
                                 @Value("${app.catalogo.snapshot-validez-horas:24}") long validezHoras) {
        this.catalogoClient = catalogoClient;
        this.snapshotRepository = snapshotRepository;
        this.validezHoras = validezHoras;
    }

    /** Para operaciones que mueven stock: el articulo tiene que existir (si no, es error de negocio). */
    public ArticuloSnapshot obtenerParaOperar(Long articuloId, List<String> advertencias) {
        Optional<ArticuloSnapshot> reglas = obtener(articuloId, advertencias);
        return reglas.orElseThrow(() -> new ReglaNegocioException("El articulo " + articuloId + " no existe en el catalogo"));
    }

    /** Para consultas por id de articulo (si no existe, es un 404). */
    public ArticuloSnapshot obtenerParaConsultar(Long articuloId) {
        return obtener(articuloId, new ArrayList<>())
                .orElseThrow(() -> new RecursoNoEncontradoException("Articulo", articuloId));
    }

    private Optional<ArticuloSnapshot> obtener(Long articuloId, List<String> advertencias) {
        try {
            Optional<ReglasArticuloRemoto> remoto = catalogoClient.obtenerReglas(articuloId);
            return remoto.map(this::guardarSnapshot);
        } catch (ServicioNoDisponibleException | ErrorRemotoException e) {
            Optional<ArticuloSnapshot> copia = snapshotRepository.findById(articuloId);
            if (copia.isPresent() && copia.get().getActualizadoEn().isAfter(LocalDateTime.now().minusHours(validezHoras))) {
                log.warn("ms-catalogo no disponible: se usa la copia local del articulo {}", articuloId);
                advertencias.add("El catalogo no respondio: el articulo " + copia.get().getSku()
                        + " se valido con la copia local del " + copia.get().getActualizadoEn().format(FORMATO));
                return copia;
            }
            throw new ServicioNoDisponibleException("No se pudo consultar el catalogo para validar el articulo "
                    + articuloId + " y no hay una copia local reciente. Reintente en unos minutos", e);
        }
    }

    /**
     * Para listados: intenta refrescar todo junto con una sola llamada; si el catalogo no responde,
     * usa las copias locales aunque no sean recientes (para mostrar informacion alcanza).
     */
    public Map<Long, ArticuloSnapshot> obtenerVarios(Collection<Long> ids) {
        Map<Long, ArticuloSnapshot> resultado = new HashMap<>();
        if (ids.isEmpty()) {
            return resultado;
        }
        try {
            for (ReglasArticuloRemoto r : catalogoClient.obtenerReglas(ids)) {
                resultado.put(r.getArticuloId(), guardarSnapshot(r));
            }
        } catch (ServicioNoDisponibleException | ErrorRemotoException e) {
            log.warn("ms-catalogo no disponible: los listados se arman con las copias locales");
            snapshotRepository.findAllById(ids).forEach(s -> resultado.put(s.getArticuloId(), s));
        }
        return resultado;
    }

    private ArticuloSnapshot guardarSnapshot(ReglasArticuloRemoto r) {
        ArticuloSnapshot s = ArticuloSnapshot.builder()
                .articuloId(r.getArticuloId())
                .sku(r.getSku())
                .nombre(r.getNombre())
                .unidadMedida(r.getUnidadMedida())
                .permiteDecimales(r.isPermiteDecimales())
                .controlaLote(r.isControlaLote())
                .controlaVencimiento(r.isControlaVencimiento())
                .stockMinimo(r.getStockMinimo())
                .activo(r.isActivo())
                .actualizadoEn(LocalDateTime.now())
                .build();
        return snapshotRepository.save(s);
    }
}
