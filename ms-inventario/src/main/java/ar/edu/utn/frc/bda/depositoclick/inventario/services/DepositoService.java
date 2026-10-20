package ar.edu.utn.frc.bda.depositoclick.inventario.services;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ConflictoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import ar.edu.utn.frc.bda.depositoclick.inventario.dtos.DepositoDTO;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.Deposito;
import ar.edu.utn.frc.bda.depositoclick.inventario.repositories.DepositoRepository;
import ar.edu.utn.frc.bda.depositoclick.inventario.repositories.ExistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepositoService {

    private final DepositoRepository depositoRepository;
    private final ExistenciaRepository existenciaRepository;

    @Transactional(readOnly = true)
    public List<DepositoDTO> listar(Boolean activo) {
        return depositoRepository.findAll(Sort.by("codigo")).stream()
                .filter(d -> activo == null || d.isActivo() == activo)
                .map(DepositoService::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepositoDTO obtener(Long id) {
        return toDTO(buscar(id));
    }

    @Transactional
    public DepositoDTO crear(DepositoDTO dto) {
        String codigo = dto.getCodigo().trim().toUpperCase();
        if (depositoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictoException("Ya existe un deposito con codigo " + codigo);
        }
        Deposito d = Deposito.builder()
                .codigo(codigo)
                .nombre(dto.getNombre().trim())
                .direccion(dto.getDireccion())
                .pais(dto.getPais().toUpperCase())
                .activo(true)
                .build();
        return toDTO(depositoRepository.save(d));
    }

    /** El codigo no se modifica. El pais si (por ejemplo si se cargo mal). */
    @Transactional
    public DepositoDTO actualizar(Long id, DepositoDTO dto) {
        Deposito d = buscar(id);
        if (!d.getCodigo().equalsIgnoreCase(dto.getCodigo().trim())) {
            throw new ReglaNegocioException("El codigo de un deposito no se puede modificar");
        }
        d.setNombre(dto.getNombre().trim());
        d.setDireccion(dto.getDireccion());
        d.setPais(dto.getPais().toUpperCase());
        return toDTO(depositoRepository.save(d));
    }

    /** Baja logica: solo si el deposito quedo vacio (si no, el stock quedaria "perdido"). */
    @Transactional
    public void desactivar(Long id) {
        Deposito d = buscar(id);
        BigDecimal total = existenciaRepository.totalEnDeposito(id);
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            throw new ConflictoException("No se puede dar de baja el deposito " + d.getCodigo()
                    + " porque todavia tiene stock (" + total.stripTrailingZeros().toPlainString()
                    + "). Transfieralo a otro deposito primero");
        }
        d.setActivo(false);
        depositoRepository.save(d);
    }

    public Deposito buscar(Long id) {
        return depositoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Deposito", id));
    }

    /** Para mover stock: el deposito tiene que existir y estar activo. */
    public Deposito buscarActivo(Long id) {
        Deposito d = depositoRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("El deposito " + id + " no existe"));
        if (!d.isActivo()) {
            throw new ReglaNegocioException("El deposito " + d.getCodigo() + " esta dado de baja");
        }
        return d;
    }

    public static DepositoDTO toDTO(Deposito d) {
        return DepositoDTO.builder()
                .id(d.getId())
                .codigo(d.getCodigo())
                .nombre(d.getNombre())
                .direccion(d.getDireccion())
                .pais(d.getPais())
                .activo(d.isActivo())
                .build();
    }
}
