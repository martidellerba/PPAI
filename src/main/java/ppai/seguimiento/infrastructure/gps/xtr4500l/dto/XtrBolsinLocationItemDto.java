package ppai.seguimiento.infrastructure.gps.xtr4500l.dto;

import java.time.LocalDateTime;

public record XtrBolsinLocationItemDto(
        Integer numeroBolsin,
        Double latitud,
        Double longitud,
        LocalDateTime fechaHoraUltimaActualizacion
) {
}
