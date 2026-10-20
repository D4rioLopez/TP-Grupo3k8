package ar.edu.utn.frc.bda.depositoclick.commons.exceptions;

import ar.edu.utn.frc.bda.depositoclick.commons.dtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo global de errores, compartido por todos los microservicios funcionales.
 * Se activa solo porque cada microservicio escanea el paquete "ar.edu.utn.frc.bda.depositoclick"
 * (ver la clase *Application de cada uno).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Nuestras excepciones de negocio: cada una sabe su codigo HTTP
    @ExceptionHandler(DepositoClickException.class)
    public ResponseEntity<ErrorResponse> handleDepositoClick(DepositoClickException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(), ex.getStatus().value(), ex.getMessage());
        }
        return armar(ex.getStatus(), ex.getMessage(), request, null);
    }

    // @Valid sobre un @RequestBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidaciones(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                errores.put(error.getObjectName(), error.getDefaultMessage()));
        return armar(HttpStatus.BAD_REQUEST, "Los datos enviados no son validos", request, errores);
    }

    // validaciones sobre @PathVariable / @RequestParam (Spring 6.1+)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidacionParametros(HandlerMethodValidationException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(r -> r.getResolvableErrors().forEach(e ->
                errores.put(r.getMethodParameter().getParameterName(), e.getDefaultMessage())));
        return armar(HttpStatus.BAD_REQUEST, "Los parametros enviados no son validos", request, errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errores.put(v.getPropertyPath().toString(), v.getMessage()));
        return armar(HttpStatus.BAD_REQUEST, "Los datos enviados no son validos", request, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return armar(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es un JSON valido o tiene valores con formato incorrecto", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoParametro(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return armar(HttpStatus.BAD_REQUEST, "El parametro '" + ex.getName() + "' tiene un valor invalido: " + ex.getValue(), request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleParametroFaltante(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return armar(HttpStatus.BAD_REQUEST, "Falta el parametro obligatorio '" + ex.getParameterName() + "'", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        return armar(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operacion", request, null);
    }

    // Dos usuarios modificaron el mismo registro al mismo tiempo (@Version)
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencia(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return armar(HttpStatus.CONFLICT, "El recurso fue modificado por otra operacion al mismo tiempo. Reintente la operacion", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridad(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violacion de integridad: {}", ex.getMostSpecificCause().getMessage());
        return armar(HttpStatus.CONFLICT, "La operacion viola una restriccion de integridad de los datos (registro duplicado o en uso)", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return armar(HttpStatus.NOT_FOUND, "No existe el recurso " + request.getRequestURI(), request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return armar(HttpStatus.METHOD_NOT_ALLOWED, "Metodo " + ex.getMethod() + " no soportado para " + request.getRequestURI(), request, null);
    }

    // Cualquier otra cosa: 500, pero sin mostrar el stacktrace al cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return armar(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request, null);
    }

    private ResponseEntity<ErrorResponse> armar(HttpStatus status, String mensaje, HttpServletRequest request,
                                                Map<String, String> detalles) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .mensaje(mensaje)
                .path(request.getRequestURI())
                .detalles(detalles)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
