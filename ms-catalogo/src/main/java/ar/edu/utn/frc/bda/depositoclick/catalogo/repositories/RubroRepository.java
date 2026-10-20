package ar.edu.utn.frc.bda.depositoclick.catalogo.repositories;

import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Rubro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RubroRepository extends JpaRepository<Rubro, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
