package ppai.seguimiento.application.usecase;

import ppai.seguimiento.application.port.CancellationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps active cancellation tokens scoped to the servlet session that created them. */
@Component
public class ConsultaCancellationRegistry {

    private final ConcurrentHashMap<Key, CancellationToken> consultations = new ConcurrentHashMap<>();

    public void register(String sessionId, UUID consultationId) {
        Key key = new Key(sessionId, consultationId);
        if (consultations.putIfAbsent(key, new CancellationToken()) != null) {
            throw new IllegalStateException("Ya existe una consulta con ese identificador.");
        }
    }

    public CancellationToken get(String sessionId, UUID consultationId) {
        CancellationToken token = consultations.get(new Key(sessionId, consultationId));
        if (token == null) {
            throw new ConsultaNoActivaException();
        }
        token.throwIfCancelled();
        return token;
    }

    public boolean cancel(String sessionId, UUID consultationId) {
        Key key = new Key(sessionId, consultationId);
        CancellationToken token = consultations.get(key);
        if (token == null || !token.cancel()) {
            return false;
        }
        consultations.remove(key, token);
        return true;
    }

    public void finish(String sessionId, UUID consultationId) {
        consultations.remove(new Key(sessionId, consultationId));
    }

    public void cancelAll(String sessionId) {
        consultations.forEach((key, token) -> {
            if (key.sessionId().equals(sessionId) && token.cancel()) {
                consultations.remove(key, token);
            }
        });
    }

    private record Key(String sessionId, UUID consultationId) { }
}
