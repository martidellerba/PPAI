package ppai.seguimiento.application.usecase;

import ppai.seguimiento.application.port.BolsinRepository;
import ppai.seguimiento.application.port.ComisionMedicaRepository;
import ppai.seguimiento.application.port.GpsTrackerPort;
import ppai.seguimiento.application.port.NotificacionSeguimientoPort;
import ppai.seguimiento.domain.model.Bolsin;
import ppai.seguimiento.domain.model.ComisionMedica;
import ppai.seguimiento.domain.model.Empleado;
import ppai.seguimiento.domain.model.EstadoBolsin;
import ppai.seguimiento.domain.model.NotificacionSeguimiento;
import ppai.seguimiento.domain.model.UbicacionBolsin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificarSeguimientoBolsinTest {

    @Mock private BolsinRepository bolsinRepository;
    @Mock private ComisionMedicaRepository comisionRepository;
    @Mock private GpsTrackerPort gpsTracker;
    @Mock private NotificacionSeguimientoPort mailPort;
    @InjectMocks private NotificarSeguimientoBolsin useCase;

    @Test
    void consultaGerenteGpsYEnviaElPayloadDeUbicacion() {
        Empleado employee = new Empleado("Ana", "Perez", "ana@example.com", new ComisionMedica("CM-CBA-01", "Cordoba"));
        Bolsin bolsin = new Bolsin(101, LocalDate.of(2026, 3, 7), "PREC-101", 2.5,
                employee.comisionMedicaAsignada(), new ComisionMedica("CM-ROS-02", "Rosario"), EstadoBolsin.ENVIADO);
        LocalDateTime gpsTime = LocalDateTime.of(2026, 3, 7, 12, 30);
        when(bolsinRepository.findSentByOriginCommission("CM-CBA-01")).thenReturn(List.of(bolsin));
        when(comisionRepository.findManagerEmailByCommissionCode("CM-ROS-02")).thenReturn(Optional.of("manager@institution.example"));
        when(gpsTracker.obtenerUbicacion(101, "CM-CBA-01"))
                .thenReturn(new UbicacionBolsin(101, -31.42, -64.18, gpsTime));

        useCase.ejecutar(employee, 101);

        ArgumentCaptor<NotificacionSeguimiento> captor = ArgumentCaptor.forClass(NotificacionSeguimiento.class);
        verify(mailPort).notificar(captor.capture());
        assertEquals(new NotificacionSeguimiento(
                101,
                "PREC-101",
                "CM-CBA-01",
                "Cordoba",
                "CM-ROS-02",
                "Rosario",
                "manager@institution.example",
                gpsTime,
                -31.42,
                -64.18
        ), captor.getValue());
    }

    @Test
    void noIntentaEnviarSiNoHayGerenteConfigurado() {
        Empleado employee = new Empleado("Ana", "Perez", "ana@example.com", new ComisionMedica("CM-CBA-01", "Cordoba"));
        Bolsin bolsin = new Bolsin(101, LocalDate.of(2026, 3, 7), "PREC-101", 2.5,
                employee.comisionMedicaAsignada(), new ComisionMedica("CM-ROS-02", "Rosario"), EstadoBolsin.ENVIADO);
        when(bolsinRepository.findSentByOriginCommission("CM-CBA-01")).thenReturn(List.of(bolsin));
        when(comisionRepository.findManagerEmailByCommissionCode("CM-ROS-02")).thenReturn(Optional.empty());

        assertThrows(GerenteComisionNoConfiguradoException.class, () -> useCase.ejecutar(employee, 101));
        verify(gpsTracker, never()).obtenerUbicacion(101, "CM-CBA-01");
        verify(mailPort, never()).notificar(org.mockito.ArgumentMatchers.any());
    }
}
