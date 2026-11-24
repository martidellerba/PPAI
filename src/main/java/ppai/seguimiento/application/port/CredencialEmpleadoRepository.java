package ppai.seguimiento.application.port;

import ppai.seguimiento.domain.model.CredencialEmpleado;

import java.util.Optional;

public interface CredencialEmpleadoRepository {
    Optional<CredencialEmpleado> findByEmail(String email);
}
