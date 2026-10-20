package ar.edu.utn.frc.bda.depositoclick.compras.repositories;

import ar.edu.utn.frc.bda.depositoclick.compras.models.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    boolean existsByCuit(String cuit);

    List<Proveedor> findByActivoOrderByRazonSocialAsc(boolean activo);
}
