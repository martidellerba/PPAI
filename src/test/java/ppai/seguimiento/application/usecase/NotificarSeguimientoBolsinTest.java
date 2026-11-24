package ppai.seguimiento.application.usecase;

import ppai.seguimiento.application.port.BolsinNotificationRepository;
import ppai.seguimiento.domain.model.Bolsin;
import ppai.seguimiento.domain.model.ComisionMedica;
import ppai.seguimiento.domain.model.Empleado;
import ppai.seguimiento.domain.model.EstadoBolsin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificarSeguimientoBolsinTest {

    @Mock
    private BolsinNotificationRepository notificationRepository;

    @InjectMocks
    private NotificarSeguimientoBolsin useCase;

    @Test
    void registraNotificacionSinCorreoNiConsultaGps() {
        Empleado employee = new Empleado("Ana", "Perez", "ana@example.com", new ComisionMedica("CM-CBA-01", "Cordoba"));
        Bolsin bolsin = new Bolsin(101, LocalDate.of(2026, 3, 7), "PREC-101", 2.5,
                employee.comisionMedicaAsignada(), new ComisionMedica("CM-ROS-02", "Rosario"), EstadoBolsin.ENVIADO);
        when(notificationRepository.findSentByOriginCommissionAndNumberForUpdate("CM-CBA-01", 101))
                .thenReturn(Optional.of(bolsin));

        NotificarSeguimientoBolsin.Resultado result = useCase.ejecutar(employee, 101);

        assertTrue(result.registradaAhora());
        assertEquals("CM-ROS-02", result.codigoComisionDestino());
        assertEquals("Rosario", result.comisionMedicaDestino());
        verify(notificationRepository).markNotificationRegistered(101);
    }

    @Test
    void noRegistraDosVecesLaMismaNotificacion() {
        Empleado employee = new Empleado("Ana", "Perez", "ana@example.com", new ComisionMedica("CM-CBA-01", "Cordoba"));
        Bolsin bolsin = new Bolsin(101, LocalDate.of(2026, 3, 7), "PREC-101", 2.5,
                employee.comisionMedicaAsignada(), new ComisionMedica("CM-ROS-02", "Rosario"),
                EstadoBolsin.ENVIADO, true);
        when(notificationRepository.findSentByOriginCommissionAndNumberForUpdate("CM-CBA-01", 101))
                .thenReturn(Optional.of(bolsin));

        assertFalse(useCase.ejecutar(employee, 101).registradaAhora());

        verify(notificationRepository, never()).markNotificationRegistered(101);
    }

    @Test
    void noRegistraBolsinesFueraDelAlcanceDelEmpleado() {
        Empleado employee = new Empleado("Ana", "Perez", "ana@example.com", new ComisionMedica("CM-CBA-01", "Cordoba"));
        when(notificationRepository.findSentByOriginCommissionAndNumberForUpdate("CM-CBA-01", 999))
                .thenReturn(Optional.empty());

        assertThrows(BolsinNoEncontradoException.class, () -> useCase.ejecutar(employee, 999));
    }
}
