package ar.edu.utn.frc.bda.depositoclick.commons.exceptions;

import org.springframework.http.HttpStatus;

/**
 * No se pudo consultar a otro componente (otro microservicio o una API externa):
 * esta caido, no responde a tiempo, etc. (503).
 * Importante: "no pude preguntar" es distinto de "me respondio que no existe".
 */
public class ServicioNoDisponibleException extends DepositoClickException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}
