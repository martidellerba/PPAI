package ppai.seguimiento.domain.model;

import java.time.LocalDateTime;

public record UbicacionBolsin(
        Integer numeroBolsin,
        Double latitud,
        Double longitud,
        LocalDateTime fechaHoraUltimaActualizacion
) {
}
