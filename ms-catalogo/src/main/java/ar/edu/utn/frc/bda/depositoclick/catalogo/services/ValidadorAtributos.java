package ar.edu.utn.frc.bda.depositoclick.catalogo.services;

import ar.edu.utn.frc.bda.depositoclick.catalogo.models.AtributoCategoria;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Categoria;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.ValorAtributo;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Valida los atributos dinamicos de un articulo contra la definicion de su categoria.
 * Es lo que hace que el catalogo sea "polimorfico": las reglas no estan en el codigo
 * sino en los datos (cada rubro define sus categorias y atributos).
 *
 * Reglas:
 * - no se aceptan atributos que la categoria no define
 * - los atributos obligatorios tienen que venir con valor
 * - cada valor tiene que respetar el tipo (NUMERO, ENTERO, FECHA, BOOLEANO, OPCION, TEXTO)
 * Junta TODOS los errores y los informa juntos (es mas comodo para el que carga los datos).
 */
@Component
public class ValidadorAtributos {

    public List<ValorAtributo> validar(Categoria categoria, Map<String, String> valoresRecibidos) {
        Map<String, String> recibidos = valoresRecibidos == null ? Map.of() : valoresRecibidos;
        List<String> errores = new ArrayList<>();

        Map<String, AtributoCategoria> definidos = new LinkedHashMap<>();
        for (AtributoCategoria a : categoria.getAtributos()) {
            definidos.put(a.getNombre(), a);
        }

        for (String nombre : recibidos.keySet()) {
            if (!definidos.containsKey(nombre)) {
                errores.add("El atributo '" + nombre + "' no existe en la categoria '" + categoria.getNombre()
                        + "' (validos: " + String.join(", ", definidos.keySet()) + ")");
            }
        }

        List<ValorAtributo> valores = new ArrayList<>();
        for (AtributoCategoria definicion : definidos.values()) {
            String valor = recibidos.get(definicion.getNombre());
            if (valor == null || valor.isBlank()) {
                if (definicion.isObligatorio()) {
                    errores.add("El atributo '" + definicion.getNombre() + "' es obligatorio para la categoria '"
                            + categoria.getNombre() + "'");
                }
                continue;
            }
            try {
                valores.add(new ValorAtributo(definicion, normalizar(definicion, valor.trim())));
            } catch (IllegalArgumentException e) {
                errores.add("Valor invalido para '" + definicion.getNombre() + "': " + e.getMessage());
            }
        }

        if (!errores.isEmpty()) {
            throw new ReglaNegocioException(String.join("; ", errores));
        }
        return valores;
    }

    // Devuelve el valor "limpio" segun el tipo, o lanza IllegalArgumentException
    private String normalizar(AtributoCategoria definicion, String valor) {
        switch (definicion.getTipo()) {
            case NUMERO -> {
                try {
                    return new BigDecimal(valor).stripTrailingZeros().toPlainString();
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("se esperaba un numero y llego '" + valor + "'");
                }
            }
            case ENTERO -> {
                try {
                    return String.valueOf(Long.parseLong(valor));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("se esperaba un numero entero y llego '" + valor + "'");
                }
            }
            case FECHA -> {
                try {
                    return LocalDate.parse(valor).toString();
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("se esperaba una fecha yyyy-MM-dd y llego '" + valor + "'");
                }
            }
            case BOOLEANO -> {
                if (valor.equalsIgnoreCase("true") || valor.equalsIgnoreCase("false")) {
                    return valor.toLowerCase();
                }
                throw new IllegalArgumentException("se esperaba true o false y llego '" + valor + "'");
            }
            case OPCION -> {
                for (String opcion : definicion.getListaOpciones()) {
                    if (opcion.equalsIgnoreCase(valor)) {
                        return opcion; // guardamos la opcion tal cual esta definida
                    }
                }
                throw new IllegalArgumentException("'" + valor + "' no es una opcion valida ("
                        + String.join(", ", definicion.getListaOpciones()) + ")");
            }
            default -> {
                if (valor.length() > 255) {
                    throw new IllegalArgumentException("el texto no puede superar los 255 caracteres");
                }
                return valor;
            }
        }
    }
}
