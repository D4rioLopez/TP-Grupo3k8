package ar.edu.utn.frc.bda.depositoclick.commons.security;

/**
 * Roles de DepositoClick. Se administran en Keycloak (realm "depositoclick") como roles de realm
 * y viajan dentro del JWT en el claim realm_access.roles.
 *
 * - ADMIN    : administra todo (catalogo, depositos, proveedores, ajustes de inventario).
 * - COMPRAS  : gestiona proveedores, ordenes de compra y la reposicion.
 * - OPERARIO : personal del deposito: ingresos, egresos, transferencias y recepcion de ordenes.
 * - VENDEDOR : punto de venta (PoS): consulta catalogo/stock y registra egresos por VENTA.
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String COMPRAS = "COMPRAS";
    public static final String OPERARIO = "OPERARIO";
    public static final String VENDEDOR = "VENDEDOR";

    public static final String[] TODOS = {ADMIN, COMPRAS, OPERARIO, VENDEDOR};

    private Roles() {
    }
}
