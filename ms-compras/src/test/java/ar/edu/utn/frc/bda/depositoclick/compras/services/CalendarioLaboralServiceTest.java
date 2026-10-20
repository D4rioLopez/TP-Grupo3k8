package ar.edu.utn.frc.bda.depositoclick.compras.services;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.ArgentinaDatosClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.NagerDateClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.FeriadoArgentinaDatos;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.FeriadoNager;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.CalculoEntregaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Prueba la logica de dias habiles y la cadena de respaldo de las APIs de feriados (todo con mocks). */
class CalendarioLaboralServiceTest {

    private NagerDateClient nager;
    private ArgentinaDatosClient argentinaDatos;
    private CalendarioCacheService cache;
    private CalendarioLaboralService service;

    // viernes
    private static final LocalDate VIERNES = LocalDate.of(2026, 11, 20);

    @BeforeEach
    void setUp() {
        nager = mock(NagerDateClient.class);
        argentinaDatos = mock(ArgentinaDatosClient.class);
        cache = mock(CalendarioCacheService.class);
        when(cache.buscarDescarga(anyString(), anyInt())).thenReturn(Optional.empty());
        service = new CalendarioLaboralService(nager, argentinaDatos, cache, 7);
    }

    @Test
    void saltaFinDeSemanaYFeriadoNacional() {
        when(nager.obtenerFeriados("AR", 2026)).thenReturn(Optional.of(List.of(
                new FeriadoNager(LocalDate.of(2026, 11, 23), "Dia de la Soberania Nacional", "Sovereignty Day", true, List.of("Public")),
                // feriado provincial: no se tiene en cuenta
                new FeriadoNager(LocalDate.of(2026, 11, 24), "Feriado provincial", "Local", false, List.of("Public")))));

        CalculoEntregaResponse r = service.sumarDiasHabiles("AR", VIERNES, 2);

        // sab 21, dom 22, lun 23 feriado -> mar 24 (1), mie 25 (2)
        assertEquals(LocalDate.of(2026, 11, 25), r.getFechaEntrega());
        assertEquals(5, r.getDiasCorridos());
        assertEquals(1, r.getFeriadosSalteados().size());
        assertTrue(r.isConfiable());
        assertEquals(CalendarioAnual.FUENTE_NAGER, r.getFuente());
    }

    @Test
    void siNagerFallaUsaArgentinaDatos() {
        when(nager.obtenerFeriados("AR", 2026)).thenThrow(new ServicioNoDisponibleException("caida"));
        when(argentinaDatos.obtenerFeriados(2026)).thenReturn(List.of(
                new FeriadoArgentinaDatos(LocalDate.of(2026, 11, 23), "trasladable", "Dia de la Soberania Nacional")));

        CalculoEntregaResponse r = service.sumarDiasHabiles("AR", VIERNES, 2);

        assertEquals(LocalDate.of(2026, 11, 25), r.getFechaEntrega());
        assertEquals(CalendarioAnual.FUENTE_ARGENTINA_DATOS, r.getFuente());
        assertTrue(r.isConfiable());
    }

    @Test
    void siTodoFallaSoloDescuentaFinesDeSemanaYAvisa() {
        when(nager.obtenerFeriados("AR", 2026)).thenThrow(new ServicioNoDisponibleException("caida"));
        when(argentinaDatos.obtenerFeriados(2026)).thenThrow(new ServicioNoDisponibleException("caida"));

        CalculoEntregaResponse r = service.sumarDiasHabiles("AR", VIERNES, 2);

        // sin feriados: lun 23 (1), mar 24 (2)
        assertEquals(LocalDate.of(2026, 11, 24), r.getFechaEntrega());
        assertFalse(r.isConfiable());
        assertEquals(CalendarioAnual.FUENTE_SOLO_FINES_DE_SEMANA, r.getFuente());
        assertFalse(r.getAdvertencias().isEmpty());
    }

    @Test
    void respuestaInesperadaSeDescarta() {
        // fechas de otro año: no le creemos y pasamos a la fuente de respaldo
        when(nager.obtenerFeriados("AR", 2026)).thenReturn(Optional.of(List.of(
                new FeriadoNager(LocalDate.of(1999, 1, 1), "x", "x", true, List.of()))));
        when(argentinaDatos.obtenerFeriados(2026)).thenReturn(List.of(
                new FeriadoArgentinaDatos(LocalDate.of(2026, 11, 23), "trasladable", "Soberania")));

        assertEquals(CalendarioAnual.FUENTE_ARGENTINA_DATOS, service.sumarDiasHabiles("AR", VIERNES, 1).getFuente());
    }

    @Test
    void paisQueLaApiNoConoce() {
        when(nager.obtenerFeriados("XX", 2026)).thenReturn(Optional.empty());
        CalculoEntregaResponse r = service.sumarDiasHabiles("xx", VIERNES, 1);
        assertEquals(LocalDate.of(2026, 11, 23), r.getFechaEntrega());
        assertFalse(r.isConfiable());
    }

    @Test
    void ceroDiasEnFinDeSemanaPasaAlLunes() {
        when(nager.obtenerFeriados("UY", 2026)).thenReturn(Optional.of(List.of(
                new FeriadoNager(LocalDate.of(2026, 1, 1), "Año Nuevo", "New Year", true, List.of()))));
        CalculoEntregaResponse r = service.sumarDiasHabiles("UY", LocalDate.of(2026, 11, 21), 0);
        assertEquals(LocalDate.of(2026, 11, 23), r.getFechaEntrega());
    }

    @Test
    void validadorCuit() {
        ValidadorCuit v = new ValidadorCuit();
        assertTrue(v.esValido("30-71234567-1"));
        assertTrue(v.esValido("20123456786"));
        assertFalse(v.esValido("20123456780"));
        assertFalse(v.esValido("123"));
    }
}
