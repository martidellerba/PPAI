package ppai.seguimiento.application.usecase;

import ppai.seguimiento.application.port.CancellationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps active cancellation tokens scoped to the servlet session that created them. */
@Component
public class ConsultaCancellationRegistry {

    private final ConcurrentHashMap<Key, ActiveConsultation> consultations = new ConcurrentHashMap<>();

    public void register(String sessionId, UUID consultationId) {
        Key key = new Key(sessionId, consultationId);
        if (consultations.putIfAbsent(key, new ActiveConsultation()) != null) {
            throw new IllegalStateException("Ya existe una consulta con ese identificador.");
        }
    }

    public CancellationToken get(String sessionId, UUID consultationId) {
        ActiveConsultation consultation = consultations.get(new Key(sessionId, consultationId));
        if (consultation == null) {
            throw new ConsultaNoActivaException();
        }
        return consultation.start();
    }

    public boolean cancel(String sessionId, UUID consultationId) {
        Key key = new Key(sessionId, consultationId);
        ActiveConsultation consultation = consultations.get(key);
        if (consultation == null) {
            return false;
        }
        return consultation.cancel(() -> consultations.remove(key, consultation));
    }

    public void finish(String sessionId, UUID consultationId) {
        consultations.remove(new Key(sessionId, consultationId));
    }

    public void cancelAll(String sessionId) {
        consultations.forEach((key, consultation) -> {
            if (key.sessionId().equals(sessionId)) {
                consultation.cancel(() -> consultations.remove(key, consultation));
            }
        });
    }

    private static final class ActiveConsultation {
        private final CancellationToken token = new CancellationToken();
        private boolean started;

        private synchronized CancellationToken start() {
            started = true;
            token.throwIfCancelled();
            return token;
        }

        private synchronized boolean cancel(Runnable removeIfNotStarted) {
            boolean cancelled = token.cancel();
            if (cancelled && !started) {
                removeIfNotStarted.run();
            }
            return cancelled;
        }
    }

    private record Key(String sessionId, UUID consultationId) { }
}
