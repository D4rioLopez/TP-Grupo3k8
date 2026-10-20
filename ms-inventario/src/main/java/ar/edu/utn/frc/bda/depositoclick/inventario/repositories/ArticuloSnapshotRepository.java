package ar.edu.utn.frc.bda.depositoclick.inventario.repositories;

import ar.edu.utn.frc.bda.depositoclick.inventario.models.ArticuloSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticuloSnapshotRepository extends JpaRepository<ArticuloSnapshot, Long> {
}
