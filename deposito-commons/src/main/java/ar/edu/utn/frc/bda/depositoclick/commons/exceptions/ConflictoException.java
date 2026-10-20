package ar.edu.utn.frc.bda.depositoclick.commons.exceptions;

import org.springframework.http.HttpStatus;

/** Conflicto con el estado actual del recurso: duplicados, concurrencia, etc. (409). */
public class ConflictoException extends DepositoClickException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
