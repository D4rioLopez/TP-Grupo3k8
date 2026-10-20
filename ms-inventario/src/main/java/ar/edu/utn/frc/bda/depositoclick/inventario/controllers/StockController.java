package ar.edu.utn.frc.bda.depositoclick.inventario.controllers;

import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.ExistenciaResponse;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.ResumenReposicionDTO;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.StockArticuloResponse;
import ar.edu.utn.frc.bda.depositoclick.inventario.services.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Consultas de existencias")
public class StockController {

    private final StockService stockService;

    @GetMapping
    @Operation(summary = "Existencias (por deposito, articulo y lote) con cantidad mayor a cero")
    public ResponseEntity<List<ExistenciaResponse>> existencias(@RequestParam(required = false) Long depositoId,
                                                                @RequestParam(required = false) Long articuloId) {
        return ResponseEntity.ok(stockService.consultarExistencias(depositoId, articuloId));
    }

    @GetMapping("/articulos/{articuloId}")
    @Operation(summary = "Stock consolidado de un articulo: total, disponible (sin vencidos), vencido y si esta bajo el minimo")
    public ResponseEntity<StockArticuloResponse> stockDeArticulo(@PathVariable Long articuloId,
                                                                 @RequestParam(required = false) Long depositoId) {
        return ResponseEntity.ok(stockService.stockDeArticulo(articuloId, depositoId));
    }

    @GetMapping("/bajo-minimo")
    @Operation(summary = "Articulos activos con stock disponible por debajo del stock minimo")
    public ResponseEntity<List<StockArticuloResponse>> bajoMinimo(@RequestParam(required = false) Long depositoId) {
        return ResponseEntity.ok(stockService.bajoMinimo(depositoId));
    }

    @GetMapping("/vencimientos")
    @Operation(summary = "Lotes que vencen en los proximos N dias (incluye vencidos con stock)")
    public ResponseEntity<List<ExistenciaResponse>> vencimientos(@RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(stockService.proximosVencimientos(dias));
    }

    @GetMapping("/resumen-reposicion")
    @Operation(summary = "Stock disponible y consumo promedio diario por articulo de un deposito (lo consume ms-compras)")
    public ResponseEntity<List<ResumenReposicionDTO>> resumenReposicion(@RequestParam Long depositoId,
                                                                        @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(stockService.resumenReposicion(depositoId, dias));
    }
}
