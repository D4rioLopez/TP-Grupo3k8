package ar.edu.utn.frc.bda.depositoclick.inventario.repositories;

import ar.edu.utn.frc.bda.depositoclick.inventario.models.OperacionRegistrada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperacionRegistradaRepository extends JpaRepository<OperacionRegistrada, Long> {

    Optional<OperacionRegistrada> findByReferencia(String referencia);
}
