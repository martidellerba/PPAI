package ppai.seguimiento.application.port;

import ppai.seguimiento.domain.model.Bolsin;

import java.util.List;

public interface BolsinRepository {
    List<Bolsin> findSentByOriginCommission(String codigoComisionMedicaOrigen);
}
