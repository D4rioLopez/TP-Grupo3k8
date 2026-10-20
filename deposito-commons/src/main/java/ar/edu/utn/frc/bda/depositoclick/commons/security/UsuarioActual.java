package ar.edu.utn.frc.bda.depositoclick.commons.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Atajo para obtener el usuario autenticado (lo usamos para auditar quien hizo cada operacion).
 */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static String nombre() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "sistema";
        }
        return auth.getName();
    }

    public static boolean tieneRol(String rol) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }
}
