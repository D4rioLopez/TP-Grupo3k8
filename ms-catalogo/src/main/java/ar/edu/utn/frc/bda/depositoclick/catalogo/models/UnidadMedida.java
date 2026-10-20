package ar.edu.utn.frc.bda.depositoclick.catalogo.models;

/**
 * Unidad en la que se cuenta el stock de un articulo.
 * Es lo que permite que el sistema sirva para cualquier rubro: una ferreteria vende
 * cable por METRO y tornillos por KILOGRAMO, un almacen vende leche por UNIDAD.
 */
public enum UnidadMedida {
    UNIDAD(false),
    PAR(false),
    CAJA(false),
    KILOGRAMO(true),
    LITRO(true),
    METRO(true),
    METRO_CUADRADO(true);

    // si se pueden mover cantidades fraccionadas (ej: 2.5 kg)
    private final boolean permiteDecimales;

    UnidadMedida(boolean permiteDecimales) {
        this.permiteDecimales = permiteDecimales;
    }

    public boolean isPermiteDecimales() {
        return permiteDecimales;
    }
}
