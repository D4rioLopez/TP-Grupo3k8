package ar.edu.utn.frc.bda.depositoclick.inventario.repositories;

import java.math.BigDecimal;

/** Proyeccion: total consumido de un articulo en un periodo. */
public interface ConsumoArticulo {

    Long getArticuloId();

    BigDecimal getTotal();
}
