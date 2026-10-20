package ar.edu.utn.frc.bda.depositoclick.inventario.models;

import java.util.EnumSet;
import java.util.Set;

/** Por que se movio el stock. Cada tipo de operacion acepta solo algunos motivos. */
public enum MotivoMovimiento {
    // ingresos
    COMPRA,
    DEVOLUCION_CLIENTE,
    STOCK_INICIAL,
    // egresos
    VENTA,
    CONSUMO_INTERNO,
    ROTURA,
    VENCIMIENTO,
    DEVOLUCION_PROVEEDOR,
    // otros
    INVENTARIO_FISICO,
    TRANSFERENCIA;

    public static final Set<MotivoMovimiento> DE_INGRESO = EnumSet.of(COMPRA, DEVOLUCION_CLIENTE, STOCK_INICIAL);

    public static final Set<MotivoMovimiento> DE_EGRESO =
            EnumSet.of(VENTA, CONSUMO_INTERNO, ROTURA, VENCIMIENTO, DEVOLUCION_PROVEEDOR);

    // los que representan "demanda real" y sirven para calcular el consumo promedio (reposicion)
    public static final Set<MotivoMovimiento> DE_CONSUMO = EnumSet.of(VENTA, CONSUMO_INTERNO);
}
