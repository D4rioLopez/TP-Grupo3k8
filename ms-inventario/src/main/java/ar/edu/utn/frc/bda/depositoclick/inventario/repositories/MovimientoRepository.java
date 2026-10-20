package ar.edu.utn.frc.bda.depositoclick.inventario.repositories;

import ar.edu.utn.frc.bda.depositoclick.inventario.models.MotivoMovimiento;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.Movimiento;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long>, MovimientoRepositoryCustom {

    List<Movimiento> findByReferenciaOrderByIdAsc(String referencia);

    @Query("""
            SELECT m.articuloId AS articuloId, SUM(m.cantidad) AS total
            FROM Movimiento m
            WHERE m.deposito.id = :depositoId AND m.tipo = :tipo AND m.motivo IN :motivos AND m.fecha >= :desde
            GROUP BY m.articuloId
            """)
    List<ConsumoArticulo> consumoPorArticulo(@Param("depositoId") Long depositoId,
                                             @Param("tipo") TipoMovimiento tipo,
                                             @Param("motivos") Collection<MotivoMovimiento> motivos,
                                             @Param("desde") LocalDateTime desde);
}
