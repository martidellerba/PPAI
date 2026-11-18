package ppai.seguimiento.domain.model;

import java.time.LocalDate;

public record Bolsin(
        Integer numeroBolsin,
        LocalDate fecha,
        String numeroPrecinto,
        Double peso,
        ComisionMedica comisionMedicaOrigen,
        ComisionMedica comisionMedicaDestino,
        EstadoBolsin estado
) {
}
