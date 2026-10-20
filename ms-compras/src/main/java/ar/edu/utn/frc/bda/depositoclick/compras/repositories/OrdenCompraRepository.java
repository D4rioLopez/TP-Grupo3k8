package ar.edu.utn.frc.bda.depositoclick.compras.repositories;

import ar.edu.utn.frc.bda.depositoclick.compras.models.EstadoOrden;
import ar.edu.utn.frc.bda.depositoclick.compras.models.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    List<OrdenCompra> findAllByOrderByIdDesc();

    List<OrdenCompra> findByEstadoOrderByIdDesc(EstadoOrden estado);

    List<OrdenCompra> findByProveedorIdOrderByIdDesc(Long proveedorId);

    List<OrdenCompra> findByProveedorIdAndEstadoOrderByIdDesc(Long proveedorId, EstadoOrden estado);

    boolean existsByProveedorIdAndEstadoIn(Long proveedorId, Collection<EstadoOrden> estados);

    // ordenes "en camino" hacia un deposito: su pendiente cuenta como stock que ya va a llegar
    @Query("SELECT o FROM OrdenCompra o WHERE o.depositoDestinoId = :depositoId AND o.estado IN :estados")
    List<OrdenCompra> buscarPorDepositoYEstados(@Param("depositoId") Long depositoId,
                                                @Param("estados") Collection<EstadoOrden> estados);
}
