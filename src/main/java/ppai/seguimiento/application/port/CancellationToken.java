package ppai.seguimiento.application.port;

import java.util.concurrent.CancellationException;

/** Shared cancellation signal for one synchronous tracking consultation. */
public final class CancellationToken {

    private enum State { ACTIVE, CANCELLED, COMPLETED }

    private State state = State.ACTIVE;
    private Runnable cancellationHandler;

    public synchronized boolean isCancelled() {
        return state == State.CANCELLED;
    }

    public synchronized void throwIfCancelled() {
        if (state == State.CANCELLED) {
            throw new CancellationException("La consulta de seguimiento fue cancelada.");
        }
    }

    public void registerCancellationHandler(Runnable handler) {
        boolean runNow;
        synchronized (this) {
            runNow = state == State.CANCELLED;
            if (state == State.ACTIVE) {
                cancellationHandler = handler;
            }
        }
        if (runNow) {
            handler.run();
        }
    }

    public synchronized void clearCancellationHandler(Runnable handler) {
        if (cancellationHandler == handler) {
            cancellationHandler = null;
        }
    }

    public boolean cancel() {
        Runnable handler;
        synchronized (this) {
            if (state != State.ACTIVE) {
                return false;
            }
            state = State.CANCELLED;
            handler = cancellationHandler;
            cancellationHandler = null;
        }
        if (handler != null) {
            handler.run();
        }
        return true;
    }

    /** Marks the query complete; a cancellation after this point is too late. */
    public synchronized boolean complete() {
        if (state != State.ACTIVE) {
            return false;
        }
        state = State.COMPLETED;
        cancellationHandler = null;
        return true;
    }
}
