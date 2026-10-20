package ar.edu.utn.frc.bda.depositoclick.catalogo.services;

import org.springframework.stereotype.Component;

/**
 * Valida el digito verificador de un codigo GTIN (EAN-8, UPC-A de 12, EAN-13, GTIN-14).
 * Sirve para detectar errores de tipeo o de lectura del scanner antes de dar de alta un articulo
 * (si el codigo esta mal, despues el PoS no lo va a encontrar nunca).
 *
 * Algoritmo (GS1): de derecha a izquierda, sin contar el verificador, los digitos se multiplican
 * alternadamente por 3 y por 1; el verificador es lo que le falta a la suma para llegar a la
 * siguiente decena.
 */
@Component
public class ValidadorCodigoBarras {

    public boolean esValido(String codigo) {
        if (codigo == null || !codigo.matches("^(\\d{8}|\\d{12,14})$")) {
            return false;
        }
        int suma = 0;
        int largo = codigo.length();
        for (int i = largo - 2; i >= 0; i--) {
            int digito = codigo.charAt(i) - '0';
            // la posicion mas cercana al verificador multiplica por 3
            boolean porTres = (largo - 2 - i) % 2 == 0;
            suma += porTres ? digito * 3 : digito;
        }
        int verificadorCalculado = (10 - (suma % 10)) % 10;
        int verificadorInformado = codigo.charAt(largo - 1) - '0';
        return verificadorCalculado == verificadorInformado;
    }
}
