package ppai.seguimiento.domain.model;

import java.time.LocalDate;

public record Bolsin(
        Integer numeroBolsin,
        LocalDate fecha,
        String numeroPrecinto,
        Double peso,
        ComisionMedica comisionMedicaOrigen,
        ComisionMedica comisionMedicaDestino,
        EstadoBolsin estado,
        boolean notificacionRegistrada
) {
    public Bolsin(
            Integer numeroBolsin,
            LocalDate fecha,
            String numeroPrecinto,
            Double peso,
            ComisionMedica comisionMedicaOrigen,
            ComisionMedica comisionMedicaDestino,
            EstadoBolsin estado
    ) {
        this(numeroBolsin, fecha, numeroPrecinto, peso, comisionMedicaOrigen, comisionMedicaDestino, estado, false);
    }
}
