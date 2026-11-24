package ppai.seguimiento.application.usecase;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaCancellationRegistryTest {

    @Test
    void eliminaUnaConsultaCanceladaAntesDeQueElEndpointLaInicie() {
        ConsultaCancellationRegistry registry = new ConsultaCancellationRegistry();
        UUID consultationId = UUID.randomUUID();

        registry.register("session-1", consultationId);

        assertTrue(registry.cancel("session-1", consultationId));
        assertThrows(
                ConsultaNoActivaException.class,
                () -> registry.get("session-1", consultationId)
        );
    }

    @Test
    void conservaLaCancelacionDeUnaConsultaQueYaEstaEnCursoHastaQueFinalice() {
        ConsultaCancellationRegistry registry = new ConsultaCancellationRegistry();
        UUID consultationId = UUID.randomUUID();

        registry.register("session-1", consultationId);
        var token = registry.get("session-1", consultationId);

        assertTrue(registry.cancel("session-1", consultationId));
        assertThrows(CancellationException.class, token::throwIfCancelled);

        registry.finish("session-1", consultationId);
        assertThrows(
                ConsultaNoActivaException.class,
                () -> registry.get("session-1", consultationId)
        );
    }
}
