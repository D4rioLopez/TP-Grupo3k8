package ar.edu.utn.frc.bda.depositoclick.inventario.repositories;

import ar.edu.utn.frc.bda.depositoclick.inventario.models.Existencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExistenciaRepository extends JpaRepository<Existencia, Long> {

    Optional<Existencia> findByDepositoIdAndArticuloIdAndLote(Long depositoId, Long articuloId, String lote);

    // un lote es el mismo en todos los depositos (para validar que tenga siempre el mismo vencimiento)
    Optional<Existencia> findFirstByArticuloIdAndLote(Long articuloId, String lote);

    // Para FEFO: primero lo que vence antes; lo que no vence va al final
    @Query("""
            SELECT e FROM Existencia e
            WHERE e.deposito.id = :depositoId AND e.articuloId = :articuloId AND e.cantidad > 0
            ORDER BY CASE WHEN e.fechaVencimiento IS NULL THEN 1 ELSE 0 END, e.fechaVencimiento, e.id
            """)
    List<Existencia> buscarConStockOrdenFefo(@Param("depositoId") Long depositoId, @Param("articuloId") Long articuloId);

    List<Existencia> findByCantidadGreaterThanOrderByArticuloIdAsc(BigDecimal cantidad);

    List<Existencia> findByDepositoIdAndCantidadGreaterThanOrderByArticuloIdAsc(Long depositoId, BigDecimal cantidad);

    List<Existencia> findByArticuloIdAndCantidadGreaterThanOrderByDepositoIdAsc(Long articuloId, BigDecimal cantidad);

    List<Existencia> findByDepositoIdAndArticuloIdAndCantidadGreaterThan(Long depositoId, Long articuloId, BigDecimal cantidad);

    @Query("SELECT COALESCE(SUM(e.cantidad), 0) FROM Existencia e WHERE e.deposito.id = :depositoId")
    BigDecimal totalEnDeposito(@Param("depositoId") Long depositoId);

    @Query("""
            SELECT e FROM Existencia e
            WHERE e.cantidad > 0 AND e.fechaVencimiento IS NOT NULL AND e.fechaVencimiento <= :limite
            ORDER BY e.fechaVencimiento, e.deposito.id
            """)
    List<Existencia> buscarQueVencenAntesDe(@Param("limite") LocalDate limite);
}
