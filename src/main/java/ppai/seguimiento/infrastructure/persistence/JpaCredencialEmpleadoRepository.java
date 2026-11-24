package ppai.seguimiento.infrastructure.persistence;

import ppai.seguimiento.application.port.CredencialEmpleadoRepository;
import ppai.seguimiento.domain.model.ComisionMedica;
import ppai.seguimiento.domain.model.CredencialEmpleado;
import ppai.seguimiento.domain.model.Empleado;
import ppai.seguimiento.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaCredencialEmpleadoRepository implements CredencialEmpleadoRepository {

    private final SpringDataUsuarioRepository repository;

    public JpaCredencialEmpleadoRepository(SpringDataUsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CredencialEmpleado> findByEmail(String email) {
        return repository.findWithCommissionByEmail(email).map(usuario -> new CredencialEmpleado(
                new Empleado(
                        usuario.getNombre(),
                        usuario.getApellido(),
                        usuario.getEmail(),
                        new ComisionMedica(
                                usuario.getComisionMedica().getCodigo(),
                                usuario.getComisionMedica().getNombre()
                        )
                ),
                usuario.getPasswordHash()
        ));
    }
}
