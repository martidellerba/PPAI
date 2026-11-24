package ppai.seguimiento.infrastructure.persistence;

import ppai.seguimiento.domain.model.EstadoBolsin;
import ppai.seguimiento.infrastructure.persistence.entity.BolsinEntity;
import ppai.seguimiento.infrastructure.persistence.entity.ComisionMedicaEntity;
import ppai.seguimiento.infrastructure.persistence.entity.UsuarioEntity;
import ppai.seguimiento.infrastructure.persistence.repository.SpringDataBolsinRepository;
import ppai.seguimiento.infrastructure.persistence.repository.SpringDataComisionMedicaRepository;
import ppai.seguimiento.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;

@Component
@Profile("dev")
public class DevelopmentDataInitializer implements CommandLineRunner {

    private final SpringDataBolsinRepository repository;
    private final SpringDataComisionMedicaRepository comisionRepository;
    private final SpringDataUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String emailUsuarioPrueba;
    private final String passwordUsuarioPrueba;
    private final String emailSegundoUsuarioPrueba;
    private final String passwordSegundoUsuarioPrueba;

    public DevelopmentDataInitializer(
            SpringDataBolsinRepository repository,
            SpringDataComisionMedicaRepository comisionRepository,
            SpringDataUsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ppai.dev.user.email:ana@example.com}") String emailUsuarioPrueba,
            @Value("${ppai.dev.user.password:demo1234}") String passwordUsuarioPrueba,
            @Value("${ppai.dev.user2.email:luis@example.com}") String emailSegundoUsuarioPrueba,
            @Value("${ppai.dev.user2.password:demo1234}") String passwordSegundoUsuarioPrueba
    ) {
        this.repository = repository;
        this.comisionRepository = comisionRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        if (emailUsuarioPrueba.isBlank() || passwordUsuarioPrueba.isBlank()
                || emailSegundoUsuarioPrueba.isBlank() || passwordSegundoUsuarioPrueba.isBlank()) {
            throw new IllegalArgumentException("Los correos y las contraseñas de prueba no pueden estar vacios.");
        }
        this.emailUsuarioPrueba = emailUsuarioPrueba.trim().toLowerCase(Locale.ROOT);
        this.passwordUsuarioPrueba = passwordUsuarioPrueba;
        this.emailSegundoUsuarioPrueba = emailSegundoUsuarioPrueba.trim().toLowerCase(Locale.ROOT);
        this.passwordSegundoUsuarioPrueba = passwordSegundoUsuarioPrueba;
    }

    @Override
    public void run(String... args) {
        saveCommissionIfMissing("CM-CBA-01", "Cordoba");
        saveCommissionIfMissing("CM-ROS-02", "Rosario");
        saveCommissionIfMissing("CM-SFE-01", "Santa Fe");
        saveCommissionIfMissing("CM-BA-01", "Buenos Aires");
        saveCommissionIfMissing("CM-MDZ-03", "Mendoza");
        saveCommissionIfMissing("CM-MDQ-01", "Mar del Plata");
        saveDemoUserIfMissing(emailUsuarioPrueba, passwordUsuarioPrueba, "Ana", "Perez", "CM-CBA-01");
        saveDemoUserIfMissing(emailSegundoUsuarioPrueba, passwordSegundoUsuarioPrueba, "Luis", "Gomez", "CM-ROS-02");
        if (!repository.existsById(101)) {
            repository.save(new BolsinEntity(
                    101,
                    LocalDate.of(2026, 3, 7),
                    "PREC-101",
                    2.5,
                    "CM-CBA-01",
                    "Cordoba",
                    "CM-ROS-02",
                    "Rosario",
                    EstadoBolsin.ENVIADO
            ));
        }
        if (!repository.existsById(102)) {
            repository.save(new BolsinEntity(
                    102,
                    LocalDate.of(2026, 3, 7),
                    "PREC-102",
                    1.8,
                    "CM-ROS-02",
                    "Rosario",
                    "CM-CBA-01",
                    "Cordoba",
                    EstadoBolsin.ENVIADO
            ));
        }
        saveIfMissing(111, "PREC-111", 2.1, "CM-ROS-02", "Rosario");
        saveIfMissing(112, "PREC-112", 1.7, "CM-SFE-01", "Santa Fe");
        saveIfMissing(113, "PREC-113", 3.0, "CM-BA-01", "Buenos Aires");
        saveIfMissing(114, "PREC-114", 2.4, "CM-MDZ-03", "Mendoza");
        saveIfMissing(115, "PREC-115", 1.9, "CM-MDQ-01", "Mar del Plata");
    }

    private void saveIfMissing(
            int numeroBolsin,
            String numeroPrecinto,
            double peso,
            String codigoDestino,
            String nombreDestino
    ) {
        if (repository.existsById(numeroBolsin)) {
            return;
        }
        repository.save(new BolsinEntity(
                numeroBolsin,
                LocalDate.of(2026, 3, 7),
                numeroPrecinto,
                peso,
                "CM-CBA-01",
                "Cordoba",
                codigoDestino,
                nombreDestino,
                EstadoBolsin.ENVIADO
        ));
    }

    private void saveCommissionIfMissing(String codigo, String nombre) {
        if (comisionRepository.existsById(codigo)) {
            return;
        }
        comisionRepository.save(new ComisionMedicaEntity(codigo, nombre));
    }

    private void saveDemoUserIfMissing(
            String email,
            String password,
            String nombre,
            String apellido,
            String codigoComision
    ) {
        if (usuarioRepository.existsById(email)) {
            return;
        }
        ComisionMedicaEntity commission = comisionRepository.findById(codigoComision).orElseThrow();
        usuarioRepository.save(new UsuarioEntity(
                email,
                passwordEncoder.encode(password),
                nombre,
                apellido,
                commission
        ));
    }
}
