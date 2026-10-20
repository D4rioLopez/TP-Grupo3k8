package ar.edu.utn.frc.bda.depositoclick.commons.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Otro componente respondio algo que no esperabamos (un 500, un JSON invalido, etc.) (502).
 */
public class ErrorRemotoException extends DepositoClickException {

    public ErrorRemotoException(String mensaje) {
        super(mensaje);
    }

    public ErrorRemotoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_GATEWAY;
    }
}
