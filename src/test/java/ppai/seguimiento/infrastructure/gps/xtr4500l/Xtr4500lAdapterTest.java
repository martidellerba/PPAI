package ppai.seguimiento.infrastructure.gps.xtr4500l;

import ppai.seguimiento.domain.model.UbicacionBolsin;
import ppai.seguimiento.application.usecase.GpsTrackerException;
import ppai.seguimiento.infrastructure.gps.xtr4500l.dto.XtrBolsinLocationItemDto;
import ppai.seguimiento.infrastructure.gps.xtr4500l.dto.XtrBolsinLocationResponseDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Xtr4500lAdapterTest {

    @Test
    void convierteLaRespuestaDelAdapteeAlModeloDeSeguimiento() {
        Xtr4500lClient adaptee = mock(Xtr4500lClient.class);
        when(adaptee.getBolsinLocation(101, "CM-CBA-01")).thenReturn(new XtrBolsinLocationResponseDto(List.of(
                new XtrBolsinLocationItemDto(
                        101,
                        -31.4201,
                        -64.1888,
                        LocalDateTime.of(2026, 3, 7, 12, 30)
                )
        )));
        Xtr4500lAdapter adapter = new Xtr4500lAdapter(adaptee);

        UbicacionBolsin ubicacion = adapter.obtenerUbicacion(101, "CM-CBA-01");

        assertEquals(
                new UbicacionBolsin(101, -31.4201, -64.1888, LocalDateTime.of(2026, 3, 7, 12, 30)),
                ubicacion
        );
    }

    @Test
    void informaCuandoLaApiNoDevuelveElBolsinSolicitado() {
        Xtr4500lClient adaptee = mock(Xtr4500lClient.class);
        when(adaptee.getBolsinLocation(101, "CM-CBA-01"))
                .thenReturn(new XtrBolsinLocationResponseDto(List.of()));
        Xtr4500lAdapter adapter = new Xtr4500lAdapter(adaptee);

        assertThrows(GpsTrackerException.class, () -> adapter.obtenerUbicacion(101, "CM-CBA-01"));
    }

    @Test
    void rechazaCoordenadasFueraDeRango() {
        Xtr4500lClient adaptee = mock(Xtr4500lClient.class);
        when(adaptee.getBolsinLocation(101, "CM-CBA-01")).thenReturn(new XtrBolsinLocationResponseDto(List.of(
                new XtrBolsinLocationItemDto(101, 91.0, -64.1888, LocalDateTime.of(2026, 3, 7, 12, 30))
        )));
        Xtr4500lAdapter adapter = new Xtr4500lAdapter(adaptee);

        assertThrows(GpsTrackerException.class, () -> adapter.obtenerUbicacion(101, "CM-CBA-01"));
    }

    @Test
    void rechazaUnElementoNuloEnLaRespuestaDelProveedor() {
        Xtr4500lClient adaptee = mock(Xtr4500lClient.class);
        when(adaptee.getBolsinLocation(101, "CM-CBA-01"))
                .thenReturn(new XtrBolsinLocationResponseDto(java.util.Arrays.asList((XtrBolsinLocationItemDto) null)));
        Xtr4500lAdapter adapter = new Xtr4500lAdapter(adaptee);

        assertThrows(GpsTrackerException.class, () -> adapter.obtenerUbicacion(101, "CM-CBA-01"));
    }
}
