package ppai.seguimiento.application.usecase;

public class ConsultaNoActivaException extends RuntimeException {
    public ConsultaNoActivaException() {
        super("La consulta no existe, ya finalizó o fue cancelada.");
    }
}
