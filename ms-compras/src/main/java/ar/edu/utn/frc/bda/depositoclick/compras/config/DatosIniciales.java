package ar.edu.utn.frc.bda.depositoclick.compras.config;

import ar.edu.utn.frc.bda.depositoclick.compras.models.ArticuloProveedor;
import ar.edu.utn.frc.bda.depositoclick.compras.models.Proveedor;
import ar.edu.utn.frc.bda.depositoclick.compras.repositories.ArticuloProveedorRepository;
import ar.edu.utn.frc.bda.depositoclick.compras.repositories.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Proveedores de ejemplo y que articulos venden.
 * Los articuloId (1..8) son los que crea ms-catalogo en su carga inicial (mismo orden).
 * Al arrancar no podemos validar contra el catalogo (no hay usuario/token), por eso estos datos
 * se cargan directo; los que se crean por la API si se validan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.datos-iniciales", havingValue = "true", matchIfMissing = true)
public class DatosIniciales implements CommandLineRunner {

    private final ProveedorRepository proveedorRepository;
    private final ArticuloProveedorRepository articuloProveedorRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (proveedorRepository.count() > 0) {
            return;
        }
        log.info("Base de compras vacia: cargando proveedores de ejemplo");

        Proveedor lactea = proveedor("30712345671", "Distribuidora Lactea del Centro SA", "pedidos@lacteacentro.com.ar", 2);
        Proveedor mayorista = proveedor("30709988774", "Mayorista de Alimentos Cordoba SA", "ventas@mayoristacba.com.ar", 5);
        Proveedor textil = proveedor("30654321988", "Textil Norte SRL", "comercial@textilnorte.com.ar", 7);
        Proveedor bulonera = proveedor("30501122331", "Bulonera Mediterranea SA", "ventas@buloneramed.com.ar", 3);
        Proveedor drogueria = proveedor("30715554441", "Drogueria del Sur SA", "pedidos@drogueriasur.com.ar", 1);

        articulo(lactea, 1L, "LAC-001", "Leche entera 1L", "950.00", true);
        articulo(mayorista, 1L, "LAC-001", "Leche entera 1L", "900.00", false);
        articulo(lactea, 2L, "LAC-002", "Yogur bebible frutilla 1L", "1400.00", true);
        articulo(mayorista, 3L, "ALM-001", "Yerba mate 1kg", "3200.00", true);
        articulo(textil, 4L, "IND-001", "Remera algodon negra M", "8500.00", true);
        articulo(textil, 5L, "IND-002", "Remera algodon blanca L", "8500.00", true);
        articulo(bulonera, 6L, "FER-001", "Tornillo autoperforante 8x1/2", "12000.00", true);
        articulo(bulonera, 7L, "FER-002", "Cable unipolar 2.5mm", "650.00", true);
        articulo(drogueria, 8L, "FAR-001", "Ibuprofeno 400mg x10", "2300.00", true);
    }

    private Proveedor proveedor(String cuit, String razonSocial, String email, int leadTime) {
        return proveedorRepository.save(Proveedor.builder().cuit(cuit).razonSocial(razonSocial).email(email)
                .leadTimeDiasHabiles(leadTime).activo(true).build());
    }

    private void articulo(Proveedor p, Long articuloId, String sku, String nombre, String precio, boolean preferido) {
        ArticuloProveedor ap = new ArticuloProveedor();
        ap.setProveedor(p);
        ap.setArticuloId(articuloId);
        ap.setSku(sku);
        ap.setNombreArticulo(nombre);
        ap.setPrecioUnitario(new BigDecimal(precio));
        ap.setPreferido(preferido);
        articuloProveedorRepository.save(ap);
    }
}
