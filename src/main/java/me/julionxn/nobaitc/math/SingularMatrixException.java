package me.julionxn.nobaitc.math;

/**
 * Se lanza cuando una matriz no admite inversa (es singular o casi singular).
 *
 * <p>Reemplaza el antiguo contrato de devolver {@code null} desde {@code inv()},
 * unificando el manejo de errores: ahora siempre se señaliza con excepción.</p>
 */
public class SingularMatrixException extends RuntimeException {
    public SingularMatrixException(String message) {
        super(message);
    }
}
