package ppai.seguimiento.application.usecase;

public class GerenteComisionNoConfiguradoException extends RuntimeException {
    public GerenteComisionNoConfiguradoException(String codigoComision) {
        super("No hay un correo de gerente configurado para la comisión " + codigoComision + ".");
    }
}
