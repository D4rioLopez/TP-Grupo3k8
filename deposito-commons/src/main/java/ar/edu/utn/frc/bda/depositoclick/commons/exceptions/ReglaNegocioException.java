package ar.edu.utn.frc.bda.depositoclick.commons.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Los datos estan bien formados pero violan una regla del dominio (422).
 * Ej: egresar mas stock del disponible, ingresar un articulo vencido, etc.
 */
public class ReglaNegocioException extends DepositoClickException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
