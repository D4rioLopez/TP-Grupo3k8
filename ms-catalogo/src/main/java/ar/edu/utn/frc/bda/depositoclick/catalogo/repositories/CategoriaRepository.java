package ar.edu.utn.frc.bda.depositoclick.catalogo.repositories;

import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByRubroId(Long rubroId);

    boolean existsByRubroIdAndNombreIgnoreCase(Long rubroId, String nombre);

    boolean existsByRubroId(Long rubroId);
}
