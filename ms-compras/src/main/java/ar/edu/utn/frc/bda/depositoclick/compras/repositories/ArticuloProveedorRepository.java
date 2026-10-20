package ar.edu.utn.frc.bda.depositoclick.compras.repositories;

import ar.edu.utn.frc.bda.depositoclick.compras.models.ArticuloProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ArticuloProveedorRepository extends JpaRepository<ArticuloProveedor, Long> {

    List<ArticuloProveedor> findByProveedorIdOrderBySkuAsc(Long proveedorId);

    Optional<ArticuloProveedor> findByProveedorIdAndArticuloId(Long proveedorId, Long articuloId);

    List<ArticuloProveedor> findByArticuloId(Long articuloId);

    // solo de proveedores activos: son los candidatos para reponer
    @Query("SELECT ap FROM ArticuloProveedor ap WHERE ap.proveedor.activo = true")
    List<ArticuloProveedor> buscarDeProveedoresActivos();
}
