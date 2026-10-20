package ar.edu.utn.frc.bda.depositoclick.compras.controllers;

import ar.edu.utn.frc.bda.depositoclick.compras.dtos.CalculoEntregaResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.FeriadosResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.services.CalendarioAnual;
import ar.edu.utn.frc.bda.depositoclick.compras.services.CalendarioLaboralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/calendario")
@RequiredArgsConstructor
@Tag(name = "Calendario laboral", description = "Feriados por pais (API publica externa) y calculo de dias habiles")
public class CalendarioController {

    private final CalendarioLaboralService calendarioService;

    @GetMapping("/feriados")
    @Operation(summary = "Feriados nacionales de un pais en un año", description = "Indica de que fuente salieron y si son confiables")
    public ResponseEntity<FeriadosResponse> feriados(@RequestParam String pais, @RequestParam int anio) {
        CalendarioAnual c = calendarioService.obtenerCalendario(pais, anio);
        return ResponseEntity.ok(FeriadosResponse.builder()
                .pais(c.getPais())
                .anio(c.getAnio())
                .fuente(c.getFuente())
                .confiable(c.isConfiable())
                .advertencia(c.getAdvertencia())
                .feriados(c.getFeriados().entrySet().stream()
                        .map(e -> new FeriadosResponse.Item(e.getKey(), e.getValue())).toList())
                .build());
    }

    @GetMapping("/fecha-entrega")
    @Operation(summary = "Sumar dias habiles a una fecha", description = "Es el calculo que se usa al emitir una orden de compra")
    public ResponseEntity<CalculoEntregaResponse> fechaEntrega(
            @RequestParam String pais,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam int diasHabiles) {
        return ResponseEntity.ok(calendarioService.sumarDiasHabiles(pais, desde == null ? LocalDate.now() : desde, diasHabiles));
    }
}
