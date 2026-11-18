package ppai.seguimiento.application.usecase;

public class GpsTrackerException extends RuntimeException {
    public GpsTrackerException(String message) { super(message); }
    public GpsTrackerException(String message, Throwable cause) { super(message, cause); }
}
