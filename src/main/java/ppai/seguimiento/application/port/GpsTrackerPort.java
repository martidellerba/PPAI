package ppai.seguimiento.application.port;

import ppai.seguimiento.domain.model.UbicacionBolsin;

public interface GpsTrackerPort {
    UbicacionBolsin obtenerUbicacion(Integer numeroBolsin, String codigoComisionMedicaOrigen);

    default UbicacionBolsin obtenerUbicacion(
            Integer numeroBolsin,
            String codigoComisionMedicaOrigen,
            CancellationToken cancellationToken
    ) {
        cancellationToken.throwIfCancelled();
        UbicacionBolsin ubicacion = obtenerUbicacion(numeroBolsin, codigoComisionMedicaOrigen);
        cancellationToken.throwIfCancelled();
        return ubicacion;
    }
}
