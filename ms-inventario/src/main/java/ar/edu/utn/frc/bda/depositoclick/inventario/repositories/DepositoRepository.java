package ar.edu.utn.frc.bda.depositoclick.inventario.repositories;

import ar.edu.utn.frc.bda.depositoclick.inventario.models.Deposito;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositoRepository extends JpaRepository<Deposito, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);
}
