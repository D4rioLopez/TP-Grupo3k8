package ar.edu.utn.frc.bda.depositoclick.compras.controllers;

import ar.edu.utn.frc.bda.depositoclick.compras.dtos.CancelacionRequest;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.OrdenRequest;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.OrdenResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.RecepcionRequest;
import ar.edu.utn.frc.bda.depositoclick.compras.models.EstadoOrden;
import ar.edu.utn.frc.bda.depositoclick.compras.services.OrdenCompraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Las transiciones de estado se modelan como sub-recursos que se CREAN (POST):
 * /emision, /recepciones, /cancelacion. Cada una es un hecho del negocio con sus propias reglas.
 */
@RestController
@RequestMapping("/api/v1/ordenes-compra")
@RequiredArgsConstructor
@Tag(name = "Ordenes de compra", description = "Ciclo de vida de las ordenes de compra: borrador, emision, recepcion y cancelacion")
public class OrdenCompraController {

    private final OrdenCompraService ordenService;

    @GetMapping
    @Operation(summary = "Listar ordenes de compra")
    public ResponseEntity<List<OrdenResponse>> listar(@RequestParam(required = false) EstadoOrden estado,
                                                      @RequestParam(required = false) Long proveedorId) {
        return ResponseEntity.ok(ordenService.listar(estado, proveedorId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una orden de compra con sus recepciones")
    public ResponseEntity<OrdenResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crear una orden en BORRADOR (ADMIN, COMPRAS)",
            description = "Valida el deposito contra ms-inventario y cada articulo contra ms-catalogo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orden creada en BORRADOR"),
            @ApiResponse(responseCode = "422", description = "Proveedor/deposito/articulo invalido o articulo que el proveedor no vende"),
            @ApiResponse(responseCode = "503", description = "Catalogo o inventario no disponible")
    })
    public ResponseEntity<OrdenResponse> crear(@Valid @RequestBody OrdenRequest request) {
        OrdenResponse creada = ordenService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.getId()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @PostMapping("/{id}/emision")
    @Operation(summary = "Emitir la orden (ADMIN, COMPRAS)",
            description = "Calcula la fecha estimada de entrega sumando el lead time del proveedor en dias habiles "
                    + "del pais del deposito (feriados obtenidos de una API publica)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden emitida"),
            @ApiResponse(responseCode = "409", description = "La orden no esta en BORRADOR"),
            @ApiResponse(responseCode = "422", description = "La fecha solicitada no es habil o el proveedor no llega")
    })
    public ResponseEntity<OrdenResponse> emitir(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.emitir(id));
    }

    @PostMapping("/{id}/recepciones")
    @Operation(summary = "Registrar la recepcion (total o parcial) de la mercaderia (ADMIN, OPERARIO)",
            description = "Ingresa el stock en ms-inventario (idempotente) y actualiza lo pendiente de la orden")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recepcion registrada"),
            @ApiResponse(responseCode = "409", description = "La orden no admite recepciones u otra recepcion simultanea"),
            @ApiResponse(responseCode = "422", description = "Cantidad mayor a la pendiente, falta lote/vencimiento, etc."),
            @ApiResponse(responseCode = "503", description = "Inventario no disponible: no se registro nada, se puede reintentar")
    })
    public ResponseEntity<OrdenResponse> recibir(@PathVariable Long id, @Valid @RequestBody RecepcionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenService.recibir(id, request));
    }

    @PostMapping("/{id}/cancelacion")
    @Operation(summary = "Cancelar la orden (ADMIN, COMPRAS)", description = "Solo si esta en BORRADOR o EMITIDA sin recepciones")
    public ResponseEntity<OrdenResponse> cancelar(@PathVariable Long id, @Valid @RequestBody CancelacionRequest request) {
        return ResponseEntity.ok(ordenService.cancelar(id, request.getMotivo()));
    }
}
