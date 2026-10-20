package ar.edu.utn.frc.bda.depositoclick.compras.models;

/**
 * Ciclo de vida de una orden de compra:
 *
 *   BORRADOR --emitir--> EMITIDA --recibir--> RECIBIDA_PARCIAL --recibir--> RECIBIDA
 *      |                    |
 *      +----cancelar--------+--> CANCELADA   (solo si no se recibio nada)
 */
public enum EstadoOrden {
    BORRADOR,
    EMITIDA,
    RECIBIDA_PARCIAL,
    RECIBIDA,
    CANCELADA;

    public boolean admiteRecepcion() {
        return this == EMITIDA || this == RECIBIDA_PARCIAL;
    }
}
