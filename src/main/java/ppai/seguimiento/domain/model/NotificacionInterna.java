package ppai.seguimiento.domain.model;

import java.time.LocalDateTime;

public record NotificacionInterna(
        Integer numeroBolsin,
        String numeroPrecinto,
        String codigoComisionMedicaOrigen,
        String comisionMedicaOrigen,
        String codigoComisionMedicaDestino,
        String comisionMedicaDestino,
        LocalDateTime fechaHora
) {
}
