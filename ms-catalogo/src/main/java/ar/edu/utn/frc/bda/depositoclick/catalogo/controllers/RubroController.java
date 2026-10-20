package ar.edu.utn.frc.bda.depositoclick.catalogo.controllers;

import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.RubroDTO;
import ar.edu.utn.frc.bda.depositoclick.catalogo.services.RubroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rubros")
@RequiredArgsConstructor
@Tag(name = "Rubros", description = "Rubros de negocio que usa el deposito (alimentos, indumentaria, ferreteria...)")
public class RubroController {

    private final RubroService rubroService;

    @GetMapping
    @Operation(summary = "Listar rubros")
    public ResponseEntity<List<RubroDTO>> listar() {
        return ResponseEntity.ok(rubroService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un rubro")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rubro encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe el rubro")
    })
    public ResponseEntity<RubroDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(rubroService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crear un rubro (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rubro creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe un rubro con ese nombre")
    })
    public ResponseEntity<RubroDTO> crear(@Valid @RequestBody RubroDTO dto) {
        RubroDTO creado = rubroService.crear(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar un rubro (ADMIN)")
    public ResponseEntity<RubroDTO> actualizar(@PathVariable Long id, @Valid @RequestBody RubroDTO dto) {
        return ResponseEntity.ok(rubroService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un rubro sin categorias (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rubro eliminado"),
            @ApiResponse(responseCode = "409", description = "El rubro tiene categorias")
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        rubroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
