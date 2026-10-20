package ar.edu.utn.frc.bda.depositoclick.catalogo.controllers;

import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.AtributoDTO;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.CategoriaRequest;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.CategoriaResponse;
import ar.edu.utn.frc.bda.depositoclick.catalogo.services.CategoriaService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
@Tag(name = "Categorias", description = "Categorias de articulos: definen los atributos dinamicos y si se controla lote/vencimiento")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    @Operation(summary = "Listar categorias, opcionalmente de un rubro")
    public ResponseEntity<List<CategoriaResponse>> listar(@RequestParam(required = false) Long rubroId) {
        return ResponseEntity.ok(categoriaService.listar(rubroId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una categoria con sus atributos")
    public ResponseEntity<CategoriaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crear una categoria con su definicion de atributos (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria creada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe una categoria con ese nombre en el rubro"),
            @ApiResponse(responseCode = "422", description = "Rubro inexistente o atributos mal definidos")
    })
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse creada = categoriaService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.getId()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @PostMapping("/{id}/atributos")
    @Operation(summary = "Agregar un atributo a una categoria (ADMIN)",
            description = "Si la categoria ya tiene articulos, el atributo nuevo no puede ser obligatorio")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Atributo agregado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un atributo con ese nombre"),
            @ApiResponse(responseCode = "422", description = "Atributo obligatorio en categoria con articulos")
    })
    public ResponseEntity<CategoriaResponse> agregarAtributo(@PathVariable Long id, @Valid @RequestBody AtributoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.agregarAtributo(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una categoria sin articulos (ADMIN)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
