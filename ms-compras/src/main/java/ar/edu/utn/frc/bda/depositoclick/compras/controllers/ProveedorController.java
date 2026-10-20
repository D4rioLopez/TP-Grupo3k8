package ar.edu.utn.frc.bda.depositoclick.compras.controllers;

import ar.edu.utn.frc.bda.depositoclick.compras.dtos.ArticuloProveedorRequest;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.ArticuloProveedorResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.ProveedorDTO;
import ar.edu.utn.frc.bda.depositoclick.compras.services.ProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Proveedores, sus plazos de entrega y los articulos que venden")
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    @Operation(summary = "Listar proveedores")
    public ResponseEntity<List<ProveedorDTO>> listar(@RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(proveedorService.listar(activo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un proveedor")
    public ResponseEntity<ProveedorDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crear un proveedor (ADMIN, COMPRAS)", description = "Se valida el digito verificador del CUIT")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proveedor creado"),
            @ApiResponse(responseCode = "409", description = "CUIT repetido"),
            @ApiResponse(responseCode = "422", description = "CUIT invalido")
    })
    public ResponseEntity<ProveedorDTO> crear(@Valid @RequestBody ProveedorDTO dto) {
        ProveedorDTO creado = proveedorService.crear(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar un proveedor (ADMIN, COMPRAS)", description = "El CUIT no se puede modificar")
    public ResponseEntity<ProveedorDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorDTO dto) {
        return ResponseEntity.ok(proveedorService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja un proveedor (ADMIN, COMPRAS)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proveedor dado de baja"),
            @ApiResponse(responseCode = "409", description = "Tiene ordenes pendientes de recepcion")
    })
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        proveedorService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/articulos")
    @Operation(summary = "Articulos que vende el proveedor, con precio pactado")
    public ResponseEntity<List<ArticuloProveedorResponse>> listarArticulos(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.listarArticulos(id));
    }

    @PostMapping("/{id}/articulos")
    @Operation(summary = "Asociar (o actualizar) un articulo al proveedor (ADMIN, COMPRAS)",
            description = "El articulo se valida contra ms-catalogo (tiene que existir y estar activo)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Articulo asociado"),
            @ApiResponse(responseCode = "422", description = "Articulo inexistente o inactivo"),
            @ApiResponse(responseCode = "503", description = "No se pudo consultar el catalogo")
    })
    public ResponseEntity<ArticuloProveedorResponse> asociarArticulo(@PathVariable Long id,
                                                                     @Valid @RequestBody ArticuloProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.asociarArticulo(id, request));
    }

    @DeleteMapping("/{id}/articulos/{articuloId}")
    @Operation(summary = "Quitar un articulo del proveedor (ADMIN, COMPRAS)")
    public ResponseEntity<Void> desasociarArticulo(@PathVariable Long id, @PathVariable Long articuloId) {
        proveedorService.desasociarArticulo(id, articuloId);
        return ResponseEntity.noContent().build();
    }
}
