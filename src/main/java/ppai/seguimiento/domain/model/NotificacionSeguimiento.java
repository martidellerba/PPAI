package ppai.seguimiento.domain.model;

import java.time.LocalDateTime;

public record NotificacionSeguimiento(
        Integer numeroBolsin,
        String numeroPrecinto,
        String codigoComisionMedicaOrigen,
        String comisionMedicaOrigen,
        String codigoComisionMedicaDestino,
        String comisionMedicaDestino,
        String emailGerenteDestino,
        LocalDateTime fechaHoraUltimaActualizacion,
        Double latitud,
        Double longitud
) {
}
