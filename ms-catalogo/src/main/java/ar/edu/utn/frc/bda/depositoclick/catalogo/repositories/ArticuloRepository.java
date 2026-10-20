package ar.edu.utn.frc.bda.depositoclick.catalogo.repositories;

import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Articulo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    Optional<Articulo> findBySkuIgnoreCase(String sku);

    Optional<Articulo> findByCodigoBarras(String codigoBarras);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);

    boolean existsByCategoriaId(Long categoriaId);

    // Busqueda con filtros opcionales: si un parametro viene null, no filtra por el.
    @Query("""
            SELECT a FROM Articulo a
            WHERE (:texto IS NULL
                   OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', CAST(:texto AS string), '%'))
                   OR LOWER(a.sku) LIKE LOWER(CONCAT('%', CAST(:texto AS string), '%')))
              AND (:categoriaId IS NULL OR a.categoria.id = :categoriaId)
              AND (:rubroId IS NULL OR a.categoria.rubro.id = :rubroId)
              AND (:activo IS NULL OR a.activo = :activo)
            """)
    Page<Articulo> buscar(@Param("texto") String texto,
                          @Param("categoriaId") Long categoriaId,
                          @Param("rubroId") Long rubroId,
                          @Param("activo") Boolean activo,
                          Pageable pageable);
}
