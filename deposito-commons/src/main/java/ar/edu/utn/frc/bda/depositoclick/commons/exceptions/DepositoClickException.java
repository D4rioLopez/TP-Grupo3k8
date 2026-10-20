package ar.edu.utn.frc.bda.depositoclick.commons.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepcion base de todas las excepciones "de negocio" de DepositoClick.
 * Cada subclase sabe que codigo HTTP le corresponde, asi el GlobalExceptionHandler
 * no tiene que conocer una por una.
 */
public abstract class DepositoClickException extends RuntimeException {

    protected DepositoClickException(String mensaje) {
        super(mensaje);
    }

    protected DepositoClickException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public abstract HttpStatus getStatus();
}
