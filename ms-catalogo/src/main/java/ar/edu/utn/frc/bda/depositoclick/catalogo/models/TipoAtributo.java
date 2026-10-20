package ar.edu.utn.frc.bda.depositoclick.catalogo.models;

/** Tipos de dato posibles para un atributo dinamico de categoria. */
public enum TipoAtributo {
    TEXTO,
    NUMERO,   // decimal, ej: contenido neto 1.5
    ENTERO,
    FECHA,    // formato ISO yyyy-MM-dd
    BOOLEANO, // true / false
    OPCION    // uno de una lista cerrada (ej: talle S, M, L, XL)
}
