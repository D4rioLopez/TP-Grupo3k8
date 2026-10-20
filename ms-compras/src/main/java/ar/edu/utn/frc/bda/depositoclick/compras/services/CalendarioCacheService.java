package ar.edu.utn.frc.bda.depositoclick.compras.services;

import ar.edu.utn.frc.bda.depositoclick.compras.models.CalendarioDescargado;
import ar.edu.utn.frc.bda.depositoclick.compras.models.Feriado;
import ar.edu.utn.frc.bda.depositoclick.compras.repositories.CalendarioDescargadoRepository;
import ar.edu.utn.frc.bda.depositoclick.compras.repositories.FeriadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Cache persistente de feriados (en la base de Compras).
 * Se guarda en una transaccion PROPIA (REQUIRES_NEW): si despues la emision de la orden falla
 * por una regla de negocio, igual nos quedamos con los feriados descargados.
 */
@Service
@RequiredArgsConstructor
public class CalendarioCacheService {

    private final FeriadoRepository feriadoRepository;
    private final CalendarioDescargadoRepository descargadoRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardar(String pais, int anio, Map<LocalDate, String> feriados, String fuente) {
        feriadoRepository.borrarPorPaisYAnio(pais, anio);
        feriados.forEach((fecha, nombre) -> feriadoRepository.save(Feriado.builder()
                .pais(pais).anio(anio).fecha(fecha)
                .nombre(nombre.length() > 120 ? nombre.substring(0, 120) : nombre)
                .build()));
        CalendarioDescargado registro = descargadoRepository.findByPaisAndAnio(pais, anio)
                .orElseGet(() -> CalendarioDescargado.builder().pais(pais).anio(anio).build());
        registro.setFuente(fuente);
        registro.setFechaDescarga(LocalDateTime.now());
        descargadoRepository.save(registro);
    }

    @Transactional(readOnly = true)
    public Optional<CalendarioDescargado> buscarDescarga(String pais, int anio) {
        return descargadoRepository.findByPaisAndAnio(pais, anio);
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, String> leer(String pais, int anio) {
        Map<LocalDate, String> mapa = new LinkedHashMap<>();
        feriadoRepository.findByPaisAndAnioOrderByFechaAsc(pais, anio).forEach(f -> mapa.put(f.getFecha(), f.getNombre()));
        return mapa;
    }
}
