package ar.edu.utn.frc.bda.depositoclick.compras.services;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ErrorRemotoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.ArgentinaDatosClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.NagerDateClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.FeriadoArgentinaDatos;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.FeriadoNager;
import ar.edu.utn.frc.bda.depositoclick.compras.dtos.CalculoEntregaResponse;
import ar.edu.utn.frc.bda.depositoclick.compras.models.CalendarioDescargado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Calendario laboral por pais, armado con informacion de APIs publicas de feriados.
 *
 * POR QUE se necesita externamente: los feriados cambian todos los años (Carnaval y Semana Santa
 * son moviles, los "trasladables" y los "puentes turisticos" se fijan por decreto) y dependen del
 * pais del deposito. No es un dato que se pueda dejar fijo en la configuracion.
 *
 * Orden de busqueda (de mejor a peor):
 *  1. cache local si es reciente (app.feriados.cache-dias)
 *  2. Nager.Date (cualquier pais)
 *  3. ArgentinaDatos (solo AR, si Nager fallo)
 *  4. cache local aunque este vencida
 *  5. sin feriados: solo se descuentan sabados y domingos, y se marca el resultado como NO confiable
 */
@Slf4j
@Service
public class CalendarioLaboralService {

    private final NagerDateClient nagerClient;
    private final ArgentinaDatosClient argentinaDatosClient;
    private final CalendarioCacheService cache;
    private final long cacheDias;

    public CalendarioLaboralService(NagerDateClient nagerClient, ArgentinaDatosClient argentinaDatosClient,
                                    CalendarioCacheService cache,
                                    @Value("${app.feriados.cache-dias:7}") long cacheDias) {
        this.nagerClient = nagerClient;
        this.argentinaDatosClient = argentinaDatosClient;
        this.cache = cache;
        this.cacheDias = cacheDias;
    }

    public CalendarioAnual obtenerCalendario(String paisRecibido, int anio) {
        String pais = validarPais(paisRecibido);
        if (anio < 2000 || anio > 2100) {
            throw new ReglaNegocioException("Año fuera de rango: " + anio);
        }

        Optional<CalendarioDescargado> descarga = cache.buscarDescarga(pais, anio);
        if (descarga.isPresent() && descarga.get().getFechaDescarga().isAfter(LocalDateTime.now().minusDays(cacheDias))) {
            return new CalendarioAnual(pais, anio, cache.leer(pais, anio), descarga.get().getFuente(), true, null);
        }

        try {
            Optional<List<FeriadoNager>> respuesta = nagerClient.obtenerFeriados(pais, anio);
            if (respuesta.isEmpty()) {
                // Nager no conoce el pais: no es una falla, otra fuente no lo va a arreglar
                return sinFeriados(pais, anio, "La API de feriados no tiene informacion del pais " + pais
                        + ": solo se descontaron sabados y domingos");
            }
            Map<LocalDate, String> feriados = new TreeMap<>();
            for (FeriadoNager f : respuesta.get()) {
                // solo los nacionales: un feriado provincial no frena una entrega en todo el pais
                if (f.isGlobal() && f.getDate() != null) {
                    feriados.merge(f.getDate(), f.getLocalName(), (a, b) -> a + " / " + b);
                }
            }
            validarRespuesta(feriados, anio, "Nager.Date");
            cache.guardar(pais, anio, feriados, CalendarioAnual.FUENTE_NAGER);
            return new CalendarioAnual(pais, anio, feriados, CalendarioAnual.FUENTE_NAGER, true, null);
        } catch (ServicioNoDisponibleException | ErrorRemotoException e) {
            log.warn("No se pudieron obtener feriados de Nager.Date ({} {}): {}", pais, anio, e.getMessage());
        }

        if (pais.equals("AR")) {
            try {
                Map<LocalDate, String> feriados = new TreeMap<>();
                for (FeriadoArgentinaDatos f : argentinaDatosClient.obtenerFeriados(anio)) {
                    if (f.getFecha() != null) {
                        feriados.merge(f.getFecha(), f.getNombre(), (a, b) -> a + " / " + b);
                    }
                }
                validarRespuesta(feriados, anio, "ArgentinaDatos");
                cache.guardar(pais, anio, feriados, CalendarioAnual.FUENTE_ARGENTINA_DATOS);
                return new CalendarioAnual(pais, anio, feriados, CalendarioAnual.FUENTE_ARGENTINA_DATOS, true,
                        "Se uso la fuente de feriados de respaldo (ArgentinaDatos)");
            } catch (ServicioNoDisponibleException | ErrorRemotoException e) {
                log.warn("Tampoco respondio ArgentinaDatos ({}): {}", anio, e.getMessage());
            }
        }

        if (descarga.isPresent()) {
            return new CalendarioAnual(pais, anio, cache.leer(pais, anio), descarga.get().getFuente() + "_CACHE", true,
                    "Las APIs de feriados no respondieron: se usaron los feriados descargados el "
                            + descarga.get().getFechaDescarga().toLocalDate());
        }
        return sinFeriados(pais, anio, "No se pudieron obtener los feriados de " + pais + " " + anio
                + ": solo se descontaron sabados y domingos (la fecha podria ser anterior a la real)");
    }

    /**
     * Suma N dias habiles a una fecha (sin contar la fecha de inicio).
     * Con 0 dias habiles, devuelve la misma fecha si es habil o el proximo dia habil.
     */
    public CalculoEntregaResponse sumarDiasHabiles(String paisRecibido, LocalDate desde, int diasHabiles) {
        String pais = validarPais(paisRecibido);
        if (diasHabiles < 0 || diasHabiles > 250) {
            throw new ReglaNegocioException("Los dias habiles deben estar entre 0 y 250");
        }
        Map<Integer, CalendarioAnual> calendarios = new HashMap<>();
        List<String> salteados = new ArrayList<>();
        LocalDate fecha = desde;
        int contados = 0;
        while (contados < diasHabiles) {
            fecha = fecha.plusDays(1);
            if (esHabil(pais, fecha, calendarios, salteados)) {
                contados++;
            }
        }
        while (diasHabiles == 0 && !esHabil(pais, fecha, calendarios, salteados)) {
            fecha = fecha.plusDays(1);
        }

        Set<String> fuentes = new LinkedHashSet<>();
        Set<String> advertencias = new LinkedHashSet<>();
        boolean confiable = true;
        for (CalendarioAnual c : calendarios.values()) {
            fuentes.add(c.getFuente());
            confiable &= c.isConfiable();
            if (c.getAdvertencia() != null) {
                advertencias.add(c.getAdvertencia());
            }
        }
        return CalculoEntregaResponse.builder()
                .pais(pais)
                .desde(desde)
                .diasHabiles(diasHabiles)
                .fechaEntrega(fecha)
                .diasCorridos(ChronoUnit.DAYS.between(desde, fecha))
                .feriadosSalteados(salteados)
                .fuente(String.join(",", fuentes))
                .confiable(confiable)
                .advertencias(advertencias.isEmpty() ? null : new ArrayList<>(advertencias))
                .build();
    }

    /** Devuelve null si la fecha es habil, o el motivo por el que no lo es. */
    public String motivoNoHabil(String pais, LocalDate fecha) {
        if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return "es sabado";
        }
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return "es domingo";
        }
        String feriado = obtenerCalendario(pais, fecha.getYear()).getFeriados().get(fecha);
        return feriado == null ? null : "es feriado (" + feriado + ")";
    }

    private boolean esHabil(String pais, LocalDate fecha, Map<Integer, CalendarioAnual> calendarios, List<String> salteados) {
        if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        CalendarioAnual calendario = calendarios.computeIfAbsent(fecha.getYear(), anio -> obtenerCalendario(pais, anio));
        String feriado = calendario.getFeriados().get(fecha);
        if (feriado != null) {
            salteados.add(fecha + " " + feriado);
            return false;
        }
        return true;
    }

    // Una respuesta "200 OK" no alcanza: si viene vacia o con fechas de otro año, no la creemos.
    private void validarRespuesta(Map<LocalDate, String> feriados, int anio, String fuente) {
        if (feriados.isEmpty()) {
            throw new ErrorRemotoException(fuente + " devolvio una lista de feriados vacia para " + anio);
        }
        boolean fechasDeOtroAnio = feriados.keySet().stream().anyMatch(f -> f.getYear() != anio);
        if (fechasDeOtroAnio) {
            throw new ErrorRemotoException(fuente + " devolvio feriados que no corresponden al año " + anio);
        }
    }

    private CalendarioAnual sinFeriados(String pais, int anio, String advertencia) {
        return new CalendarioAnual(pais, anio, Map.of(), CalendarioAnual.FUENTE_SOLO_FINES_DE_SEMANA, false, advertencia);
    }

    private String validarPais(String pais) {
        if (pais == null || !pais.matches("^[A-Za-z]{2}$")) {
            throw new ReglaNegocioException("El pais debe ser un codigo ISO de 2 letras");
        }
        return pais.toUpperCase();
    }
}
