package ppai.seguimiento.application.usecase;

import ppai.seguimiento.application.port.CredencialEmpleadoRepository;
import ppai.seguimiento.domain.model.Empleado;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AutenticarEmpleado {

    private final CredencialEmpleadoRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;

    public AutenticarEmpleado(
            CredencialEmpleadoRepository credencialRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.credencialRepository = credencialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Empleado ejecutar(String email, String password) {
        if (email == null || password == null) {
            throw new CredencialesInvalidasException();
        }
        return credencialRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(credencial -> passwordEncoder.matches(password, credencial.passwordHash()))
                .map(credencial -> credencial.empleado())
                .orElseThrow(CredencialesInvalidasException::new);
    }
}
