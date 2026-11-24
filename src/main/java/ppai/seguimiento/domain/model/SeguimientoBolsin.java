package ppai.seguimiento.domain.model;

import java.time.LocalDateTime;
import java.time.LocalDate;

public record SeguimientoBolsin(
        Integer numeroBolsin,
        String codigoComisionMedicaDestino,
        String comisionMedicaDestino,
        String numeroPrecinto,
        LocalDate fechaEnvio,
        Double peso,
        EstadoBolsin estado,
        Double latitud,
        Double longitud,
        LocalDateTime fechaHoraUltimaActualizacion,
        boolean notificacionRegistrada
) {
    public SeguimientoBolsin(
            Integer numeroBolsin,
            String codigoComisionMedicaDestino,
            String comisionMedicaDestino,
            String numeroPrecinto,
            LocalDate fechaEnvio,
            Double peso,
            EstadoBolsin estado,
            Double latitud,
            Double longitud,
            LocalDateTime fechaHoraUltimaActualizacion
    ) {
        this(numeroBolsin, codigoComisionMedicaDestino, comisionMedicaDestino, numeroPrecinto, fechaEnvio, peso,
                estado, latitud, longitud, fechaHoraUltimaActualizacion, false);
    }
}
