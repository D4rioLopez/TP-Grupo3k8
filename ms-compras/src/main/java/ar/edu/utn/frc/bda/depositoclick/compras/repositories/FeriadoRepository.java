package ar.edu.utn.frc.bda.depositoclick.compras.repositories;

import ar.edu.utn.frc.bda.depositoclick.compras.models.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeriadoRepository extends JpaRepository<Feriado, Long> {

    List<Feriado> findByPaisAndAnioOrderByFechaAsc(String pais, int anio);

    @Modifying
    @Query("DELETE FROM Feriado f WHERE f.pais = :pais AND f.anio = :anio")
    void borrarPorPaisYAnio(@Param("pais") String pais, @Param("anio") int anio);
}
