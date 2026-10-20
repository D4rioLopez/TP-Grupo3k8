package ar.edu.utn.frc.bda.depositoclick.compras.clients;

import ar.edu.utn.frc.bda.depositoclick.commons.dtos.ErrorResponse;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ConflictoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ErrorRemotoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Traduce los errores 4xx de OTRO microservicio de DepositoClick a excepciones propias.
 * Como todos los servicios responden los errores con el mismo formato (ErrorResponse de
 * deposito-commons), podemos leer el "mensaje" original y mostrarselo al usuario.
 */
final class ErroresRemotos {

    private ErroresRemotos() {
    }

    static RuntimeException traducir(String servicio, HttpClientErrorException e) {
        String mensaje = leerMensaje(e);
        return switch (e.getStatusCode().value()) {
            case 400, 404, 422 -> new ReglaNegocioException(servicio + ": " + mensaje);
            case 409 -> new ConflictoException(servicio + ": " + mensaje);
            case 401, 403 -> new AccessDeniedException(servicio + " no autorizo la operacion: " + mensaje);
            default -> new ErrorRemotoException(servicio + " respondio " + e.getStatusCode().value() + ": " + mensaje);
        };
    }

    private static String leerMensaje(HttpClientErrorException e) {
        try {
            ErrorResponse error = e.getResponseBodyAs(ErrorResponse.class);
            if (error != null && error.getMensaje() != null) {
                return error.getMensaje();
            }
        } catch (RuntimeException ignorada) {
            // el cuerpo no era un ErrorResponse: usamos el texto del status
        }
        return e.getStatusText();
    }
}
