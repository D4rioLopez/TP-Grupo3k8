package ar.edu.utn.frc.bda.depositoclick.inventario.repositories;

import ar.edu.utn.frc.bda.depositoclick.inventario.models.Movimiento;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Busqueda con multiples filtros opcionales: como no se expresa bien con una query derivada,
 * se implementa con un repositorio custom + EntityManager (patron del Apunte 16).
 */
public interface MovimientoRepositoryCustom {

    Page<Movimiento> buscar(Long depositoId, Long articuloId, TipoMovimiento tipo, String referencia,
                            LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}
