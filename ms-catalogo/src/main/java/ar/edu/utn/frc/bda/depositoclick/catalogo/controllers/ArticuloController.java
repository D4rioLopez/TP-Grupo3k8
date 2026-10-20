package ar.edu.utn.frc.bda.depositoclick.catalogo.controllers;

import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.ArticuloRequest;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.ArticuloResponse;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.ReglasStockResponse;
import ar.edu.utn.frc.bda.depositoclick.catalogo.services.ArticuloService;
import ar.edu.utn.frc.bda.depositoclick.commons.dtos.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/articulos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Articulos", description = "Articulos del catalogo (dato maestro)")
public class ArticuloController {

    private final ArticuloService articuloService;

    @GetMapping
    @Operation(summary = "Buscar articulos (paginado)", description = "Todos los filtros son opcionales. 'texto' busca en nombre y SKU")
    public ResponseEntity<PaginaResponse<ArticuloResponse>> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long rubroId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
        return ResponseEntity.ok(articuloService.buscar(texto, categoriaId, rubroId, activo,
                PageRequest.of(pagina, tamanio, Sort.by("nombre"))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un articulo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Articulo encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe el articulo")
    })
    public ResponseEntity<ArticuloResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(articuloService.obtener(id));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Obtener un articulo por SKU")
    public ResponseEntity<ArticuloResponse> obtenerPorSku(@PathVariable String sku) {
        return ResponseEntity.ok(articuloService.obtenerPorSku(sku));
    }

    @GetMapping("/codigo-barras/{codigo}")
    @Operation(summary = "Obtener un articulo por codigo de barras (lo usa el punto de venta al escanear)")
    public ResponseEntity<ArticuloResponse> obtenerPorCodigoBarras(@PathVariable String codigo) {
        return ResponseEntity.ok(articuloService.obtenerPorCodigoBarras(codigo));
    }

    @GetMapping("/{id}/reglas-stock")
    @Operation(summary = "Reglas de stock de un articulo",
            description = "Contrato para otros microservicios: unidad, si permite decimales, si controla lote/vencimiento, stock minimo y si esta activo")
    public ResponseEntity<ReglasStockResponse> reglas(@PathVariable Long id) {
        return ResponseEntity.ok(articuloService.obtenerReglas(id));
    }

    @GetMapping("/reglas-stock")
    @Operation(summary = "Reglas de stock de varios articulos (?ids=1,2,3)",
            description = "Version en lote para evitar N llamadas. Los ids inexistentes se omiten de la respuesta")
    public ResponseEntity<List<ReglasStockResponse>> reglasEnLote(@RequestParam @NotEmpty List<Long> ids) {
        return ResponseEntity.ok(articuloService.obtenerReglas(ids));
    }

    @PostMapping
    @Operation(summary = "Crear un articulo (ADMIN)", description = "Los atributos se validan contra la definicion de su categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Articulo creado"),
            @ApiResponse(responseCode = "400", description = "Datos con formato invalido"),
            @ApiResponse(responseCode = "409", description = "SKU o codigo de barras repetido"),
            @ApiResponse(responseCode = "422", description = "Atributos invalidos, categoria inexistente, codigo de barras con verificador incorrecto, etc.")
    })
    public ResponseEntity<ArticuloResponse> crear(@Valid @RequestBody ArticuloRequest request) {
        ArticuloResponse creado = articuloService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar un articulo (ADMIN)", description = "El SKU y la unidad de medida no se pueden modificar")
    public ResponseEntity<ArticuloResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ArticuloRequest request) {
        return ResponseEntity.ok(articuloService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja (logica) un articulo (ADMIN)",
            description = "El articulo queda inactivo: no puede recibir stock ni comprarse, pero se puede seguir despachando lo que haya")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Articulo dado de baja"),
            @ApiResponse(responseCode = "404", description = "No existe el articulo")
    })
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        articuloService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivacion")
    @Operation(summary = "Reactivar un articulo dado de baja (ADMIN)")
    public ResponseEntity<ArticuloResponse> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(articuloService.reactivar(id));
    }
}
