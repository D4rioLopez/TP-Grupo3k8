package ar.edu.utn.frc.bda.depositoclick.inventario.config;

import ar.edu.utn.frc.bda.depositoclick.inventario.models.Deposito;
import ar.edu.utn.frc.bda.depositoclick.inventario.repositories.DepositoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Depositos de ejemplo. El stock inicial NO se carga aca: se carga con ingresos (motivo STOCK_INICIAL)
 * a traves de la API, para que pase por todas las validaciones contra el catalogo
 * (ver scripts/demo.sh).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.datos-iniciales", havingValue = "true", matchIfMissing = true)
public class DatosIniciales implements CommandLineRunner {

    private final DepositoRepository depositoRepository;

    @Override
    public void run(String... args) {
        if (depositoRepository.count() > 0) {
            return;
        }
        log.info("Base de inventario vacia: cargando depositos de ejemplo");
        depositoRepository.save(Deposito.builder().codigo("DEP-CBA").nombre("Deposito Central Cordoba")
                .direccion("Av. Circunvalacion 1234, Cordoba").pais("AR").activo(true).build());
        depositoRepository.save(Deposito.builder().codigo("DEP-MVD").nombre("Deposito Montevideo")
                .direccion("Camino Carrasco 5678, Montevideo").pais("UY").activo(true).build());
    }
}
