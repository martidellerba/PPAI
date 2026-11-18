package ppai.seguimiento.application.usecase;

public class BolsinNoEncontradoException extends RuntimeException {
    public BolsinNoEncontradoException(Integer numeroBolsin) {
        super("No se encontro el bolsin enviado " + numeroBolsin + " para la comision del empleado.");
    }
}
