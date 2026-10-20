package ar.edu.utn.frc.bda.depositoclick.inventario.controllers;

import ar.edu.utn.frc.bda.depositoclick.commons.dtos.PaginaResponse;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.AjusteRequest;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.EgresoRequest;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.IngresoRequest;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.MovimientoResponse;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.OperacionResponse;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.TransferenciaRequest;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.TipoMovimiento;
import ar.edu.utn.frc.bda.depositoclick.inventario.services.MovimientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/movimientos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Movimientos", description = "Ingresos, egresos, transferencias y ajustes de stock")
public class MovimientoController {

    private final MovimientoService movimientoService;

    @PostMapping("/ingresos")
    @Operation(summary = "Registrar un ingreso de mercaderia (OPERARIO, ADMIN)",
            description = "Valida cada articulo contra el catalogo (activo, unidad, lote y vencimiento). "
                    + "Si la referencia ya fue procesada, devuelve 200 con el resultado original (idempotente)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ingreso registrado"),
            @ApiResponse(responseCode = "200", description = "La referencia ya habia sido procesada: no se volvio a aplicar"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio violada (lote faltante, mercaderia vencida, articulo inactivo...)"),
            @ApiResponse(responseCode = "503", description = "No se pudo validar con el catalogo y no hay copia local reciente")
    })
    public ResponseEntity<OperacionResponse> ingresar(@Valid @RequestBody IngresoRequest request) {
        return responder(movimientoService.registrarIngreso(request));
    }

    @PostMapping("/egresos")
    @Operation(summary = "Registrar un egreso (VENDEDOR solo VENTA; OPERARIO y ADMIN cualquier motivo)",
            description = "Si no se indica lote se despacha por FEFO. Lo vencido no se despacha (salvo motivo VENCIMIENTO)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Egreso registrado"),
            @ApiResponse(responseCode = "200", description = "La referencia ya habia sido procesada"),
            @ApiResponse(responseCode = "403", description = "El rol no permite ese motivo"),
            @ApiResponse(responseCode = "409", description = "Otra operacion modifico el mismo stock al mismo tiempo"),
            @ApiResponse(responseCode = "422", description = "Stock insuficiente u otra regla de negocio")
    })
    public ResponseEntity<OperacionResponse> egresar(@Valid @RequestBody EgresoRequest request) {
        return responder(movimientoService.registrarEgreso(request));
    }

    @PostMapping("/transferencias")
    @Operation(summary = "Transferir stock entre depositos (OPERARIO, ADMIN)",
            description = "Sale del origen por FEFO y entra al destino conservando lote y vencimiento, en una sola transaccion")
    public ResponseEntity<OperacionResponse> transferir(@Valid @RequestBody TransferenciaRequest request) {
        return responder(movimientoService.registrarTransferencia(request));
    }

    @PostMapping("/ajustes")
    @Operation(summary = "Ajuste por inventario fisico (ADMIN)",
            description = "Se informa la cantidad CONTADA y el sistema registra la diferencia como ajuste positivo o negativo")
    public ResponseEntity<OperacionResponse> ajustar(@Valid @RequestBody AjusteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movimientoService.registrarAjuste(request));
    }

    @GetMapping
    @Operation(summary = "Buscar movimientos (paginado, filtros opcionales)")
    public ResponseEntity<PaginaResponse<MovimientoResponse>> buscar(
            @RequestParam(required = false) Long depositoId,
            @RequestParam(required = false) Long articuloId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) String referencia,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
        return ResponseEntity.ok(movimientoService.buscar(depositoId, articuloId, tipo, referencia, desde, hasta,
                PageRequest.of(pagina, tamanio)));
    }

    @GetMapping("/operaciones/{referencia}")
    @Operation(summary = "Obtener una operacion (y sus movimientos) por referencia")
    public ResponseEntity<OperacionResponse> obtenerOperacion(@PathVariable String referencia) {
        return ResponseEntity.ok(movimientoService.obtenerOperacion(referencia));
    }

    private ResponseEntity<OperacionResponse> responder(OperacionResponse respuesta) {
        // 201 si se creo ahora, 200 si era una repeticion de algo ya procesado
        return ResponseEntity.status(respuesta.isRepetida() ? HttpStatus.OK : HttpStatus.CREATED).body(respuesta);
    }
}
