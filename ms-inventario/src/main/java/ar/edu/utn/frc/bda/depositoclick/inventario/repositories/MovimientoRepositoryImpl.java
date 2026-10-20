package ar.edu.utn.frc.bda.depositoclick.inventario.repositories;

import ar.edu.utn.frc.bda.depositoclick.inventario.models.Movimiento;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.TipoMovimiento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MovimientoRepositoryImpl implements MovimientoRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Movimiento> buscar(Long depositoId, Long articuloId, TipoMovimiento tipo, String referencia,
                                   LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        // Armamos el WHERE solo con los filtros que vinieron
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> parametros = new HashMap<>();
        if (depositoId != null) {
            where.append(" AND m.deposito.id = :depositoId");
            parametros.put("depositoId", depositoId);
        }
        if (articuloId != null) {
            where.append(" AND m.articuloId = :articuloId");
            parametros.put("articuloId", articuloId);
        }
        if (tipo != null) {
            where.append(" AND m.tipo = :tipo");
            parametros.put("tipo", tipo);
        }
        if (referencia != null) {
            where.append(" AND m.referencia = :referencia");
            parametros.put("referencia", referencia);
        }
        if (desde != null) {
            where.append(" AND m.fecha >= :desde");
            parametros.put("desde", desde);
        }
        if (hasta != null) {
            where.append(" AND m.fecha <= :hasta");
            parametros.put("hasta", hasta);
        }

        TypedQuery<Movimiento> query = em.createQuery(
                "SELECT m FROM Movimiento m" + where + " ORDER BY m.fecha DESC, m.id DESC", Movimiento.class);
        TypedQuery<Long> count = em.createQuery("SELECT COUNT(m) FROM Movimiento m" + where, Long.class);
        parametros.forEach((k, v) -> {
            query.setParameter(k, v);
            count.setParameter(k, v);
        });

        List<Movimiento> contenido = query
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        return new PageImpl<>(contenido, pageable, count.getSingleResult());
    }
}
