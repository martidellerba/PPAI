package ppai.seguimiento.application.port;

import java.util.Optional;

public interface ComisionMedicaRepository {
    Optional<String> findManagerEmailByCommissionCode(String codigoComisionMedica);
}
