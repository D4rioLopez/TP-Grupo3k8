package ar.edu.utn.frc.bda.depositoclick.inventario.models;

public enum TipoMovimiento {
    INGRESO,
    EGRESO,
    AJUSTE_POSITIVO,
    AJUSTE_NEGATIVO,
    TRANSFERENCIA_SALIDA,
    TRANSFERENCIA_ENTRADA;

    // si el movimiento suma stock a la existencia
    public boolean suma() {
        return this == INGRESO || this == AJUSTE_POSITIVO || this == TRANSFERENCIA_ENTRADA;
    }
}
