package ar.edu.utn.frc.bda.depositoclick.catalogo.config;

import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Articulo;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.AtributoCategoria;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Categoria;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Rubro;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.TipoAtributo;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.UnidadMedida;
import ar.edu.utn.frc.bda.depositoclick.catalogo.repositories.ArticuloRepository;
import ar.edu.utn.frc.bda.depositoclick.catalogo.repositories.CategoriaRepository;
import ar.edu.utn.frc.bda.depositoclick.catalogo.repositories.RubroRepository;
import ar.edu.utn.frc.bda.depositoclick.catalogo.services.ValidadorAtributos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Carga un catalogo de ejemplo la primera vez que arranca (si la base esta vacia).
 * Muestra que el mismo sistema sirve para rubros muy distintos.
 *
 * IMPORTANTE: el orden de carga define los ids de los articulos (1 a 8), que son los que usan
 * los datos de ejemplo de ms-compras (proveedores y sus articulos).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.datos-iniciales", havingValue = "true", matchIfMissing = true)
public class DatosIniciales implements CommandLineRunner {

    private final RubroRepository rubroRepository;
    private final CategoriaRepository categoriaRepository;
    private final ArticuloRepository articuloRepository;
    private final ValidadorAtributos validadorAtributos;

    @Override
    @Transactional
    public void run(String... args) {
        if (rubroRepository.count() > 0) {
            return;
        }
        log.info("Base de catalogo vacia: cargando datos de ejemplo");

        Rubro alimentos = rubroRepository.save(Rubro.builder().nombre("Alimentos").descripcion("Almacen y lacteos").build());
        Rubro indumentaria = rubroRepository.save(Rubro.builder().nombre("Indumentaria").descripcion("Ropa").build());
        Rubro ferreteria = rubroRepository.save(Rubro.builder().nombre("Ferreteria").descripcion("Bulonera y electricidad").build());
        Rubro farmacia = rubroRepository.save(Rubro.builder().nombre("Farmacia").descripcion("Medicamentos de venta libre").build());

        Categoria lacteos = categoria("Lacteos", alimentos, true, true,
                new AtributoCategoria("marca", TipoAtributo.TEXTO, true, null),
                new AtributoCategoria("contenidoNeto", TipoAtributo.NUMERO, true, null),
                new AtributoCategoria("refrigerado", TipoAtributo.BOOLEANO, true, null));
        Categoria almacen = categoria("Almacen", alimentos, true, true,
                new AtributoCategoria("marca", TipoAtributo.TEXTO, true, null));
        Categoria remeras = categoria("Remeras", indumentaria, false, false,
                new AtributoCategoria("talle", TipoAtributo.OPCION, true, "S,M,L,XL"),
                new AtributoCategoria("color", TipoAtributo.TEXTO, true, null),
                new AtributoCategoria("material", TipoAtributo.TEXTO, false, null));
        Categoria tornilleria = categoria("Tornilleria", ferreteria, false, false,
                new AtributoCategoria("medida", TipoAtributo.TEXTO, true, null),
                new AtributoCategoria("material", TipoAtributo.OPCION, true, "ACERO,BRONCE,ACERO_INOXIDABLE"));
        Categoria cables = categoria("Cables", ferreteria, true, false,
                new AtributoCategoria("seccionMm2", TipoAtributo.NUMERO, true, null),
                new AtributoCategoria("color", TipoAtributo.TEXTO, false, null));
        Categoria medicamentos = categoria("Medicamentos", farmacia, true, true,
                new AtributoCategoria("droga", TipoAtributo.TEXTO, true, null),
                new AtributoCategoria("requiereReceta", TipoAtributo.BOOLEANO, true, null));

        articulo("LAC-001", "7790001000019", "Leche entera 1L", lacteos, UnidadMedida.UNIDAD, "24",
                Map.of("marca", "La Serenisima", "contenidoNeto", "1", "refrigerado", "true"));
        articulo("LAC-002", null, "Yogur bebible frutilla 1L", lacteos, UnidadMedida.UNIDAD, "12",
                Map.of("marca", "Ilolay", "contenidoNeto", "1", "refrigerado", "true"));
        articulo("ALM-001", null, "Yerba mate 1kg", almacen, UnidadMedida.UNIDAD, "10",
                Map.of("marca", "Playadito"));
        articulo("IND-001", null, "Remera algodon negra M", remeras, UnidadMedida.UNIDAD, "5",
                Map.of("talle", "M", "color", "Negro", "material", "Algodon"));
        articulo("IND-002", null, "Remera algodon blanca L", remeras, UnidadMedida.UNIDAD, "5",
                Map.of("talle", "L", "color", "Blanco"));
        articulo("FER-001", null, "Tornillo autoperforante 8x1/2", tornilleria, UnidadMedida.KILOGRAMO, "2.5",
                Map.of("medida", "8x1/2", "material", "ACERO"));
        articulo("FER-002", null, "Cable unipolar 2.5mm", cables, UnidadMedida.METRO, "100",
                Map.of("seccionMm2", "2.5", "color", "Marron"));
        articulo("FAR-001", null, "Ibuprofeno 400mg x10", medicamentos, UnidadMedida.CAJA, "20",
                Map.of("droga", "Ibuprofeno", "requiereReceta", "false"));
    }

    private Categoria categoria(String nombre, Rubro rubro, boolean lote, boolean vencimiento, AtributoCategoria... atributos) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setRubro(rubro);
        c.setControlaLote(lote);
        c.setControlaVencimiento(vencimiento);
        for (AtributoCategoria a : atributos) {
            c.agregarAtributo(a);
        }
        return categoriaRepository.save(c);
    }

    private void articulo(String sku, String codigoBarras, String nombre, Categoria categoria, UnidadMedida unidad,
                          String stockMinimo, Map<String, String> atributos) {
        Articulo a = new Articulo();
        a.setSku(sku);
        a.setCodigoBarras(codigoBarras);
        a.setNombre(nombre);
        a.setCategoria(categoria);
        a.setUnidadMedida(unidad);
        a.setStockMinimo(new BigDecimal(stockMinimo));
        a.setActivo(true);
        a.setFechaAlta(LocalDateTime.now());
        a.reemplazarValores(validadorAtributos.validar(categoria, atributos));
        articuloRepository.save(a);
    }
}
