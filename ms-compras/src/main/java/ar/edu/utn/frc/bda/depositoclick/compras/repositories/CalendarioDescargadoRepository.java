package ar.edu.utn.frc.bda.depositoclick.compras.repositories;

import ar.edu.utn.frc.bda.depositoclick.compras.models.CalendarioDescargado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CalendarioDescargadoRepository extends JpaRepository<CalendarioDescargado, Long> {

    Optional<CalendarioDescargado> findByPaisAndAnio(String pais, int anio);
}
