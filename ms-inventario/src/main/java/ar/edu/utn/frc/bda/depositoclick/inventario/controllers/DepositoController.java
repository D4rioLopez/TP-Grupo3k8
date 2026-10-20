package ar.edu.utn.frc.bda.depositoclick.inventario.controllers;

import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.DepositoDTO;
import ar.edu.utn.frc.bda.depositoclick.inventario.services.DepositoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/depositos")
@RequiredArgsConstructor
@Tag(name = "Depositos", description = "Depositos fisicos donde se guarda el stock")
public class DepositoController {

    private final DepositoService depositoService;

    @GetMapping
    @Operation(summary = "Listar depositos")
    public ResponseEntity<List<DepositoDTO>> listar(@RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(depositoService.listar(activo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un deposito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposito encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe el deposito")
    })
    public ResponseEntity<DepositoDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(depositoService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crear un deposito (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Deposito creado"),
            @ApiResponse(responseCode = "409", description = "Codigo repetido")
    })
    public ResponseEntity<DepositoDTO> crear(@Valid @RequestBody DepositoDTO dto) {
        DepositoDTO creado = depositoService.crear(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar un deposito (ADMIN)", description = "El codigo no se puede modificar")
    public ResponseEntity<DepositoDTO> actualizar(@PathVariable Long id, @Valid @RequestBody DepositoDTO dto) {
        return ResponseEntity.ok(depositoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja un deposito vacio (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deposito dado de baja"),
            @ApiResponse(responseCode = "409", description = "El deposito todavia tiene stock")
    })
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        depositoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
