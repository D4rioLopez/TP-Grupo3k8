package ar.edu.utn.frc.bda.depositoclick.compras.controllers;

import ar.edu.utn.frc.bda.depositoclick.compras.dtos.OrdenResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.SugerenciasResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.services.ReposicionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reposicion")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reposicion", description = "Calculo de que, cuanto y a quien pedir para un deposito")
public class ReposicionController {

    private final ReposicionService reposicionService;

    @GetMapping("/sugerencias")
    @Operation(summary = "Sugerencias de reposicion de un deposito (ADMIN, COMPRAS)",
            description = "Combina stock y consumo (ms-inventario), stock minimo (ms-catalogo), proveedor y lead time (propio) "
                    + "y feriados del pais del deposito (API publica)")
    public ResponseEntity<SugerenciasResponse> sugerencias(
            @RequestParam Long depositoId,
            @RequestParam(defaultValue = "15") @Min(1) @Max(120) int diasCobertura,
            @RequestParam(defaultValue = "30") @Min(7) @Max(180) int diasHistoria) {
        return ResponseEntity.ok(reposicionService.calcularSugerencias(depositoId, diasCobertura, diasHistoria));
    }

    @PostMapping("/ordenes-borrador")
    @Operation(summary = "Generar ordenes de compra en BORRADOR a partir de las sugerencias (ADMIN, COMPRAS)",
            description = "Crea una orden por proveedor. Quedan en BORRADOR para que alguien las revise y las emita")
    public ResponseEntity<List<OrdenResponse>> generarBorradores(
            @RequestParam Long depositoId,
            @RequestParam(defaultValue = "15") @Min(1) @Max(120) int diasCobertura,
            @RequestParam(defaultValue = "30") @Min(7) @Max(180) int diasHistoria) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reposicionService.generarBorradores(depositoId, diasCobertura, diasHistoria));
    }
}
