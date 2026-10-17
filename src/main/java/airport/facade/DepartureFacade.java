package airport.facade;
import airport.common.*;
import airport.intake.Kiosk;
import airport.processing.BaggageHandler;
import airport.finalization.GateController;
import java.io.IOException;
import java.nio.file.Path;
import static airport.facade.ChainRepository.Stage.*;
public class DepartureFacade {
    private final Kiosk kiosk;
    private final BaggageHandler baggage;
    private final GateController gate;
    private final ChainRepository chains;
    public DepartureFacade(Path root) {
        kiosk = new Kiosk(root); baggage = new BaggageHandler(root);
        gate = new GateController(root); chains = new ChainRepository(root);
    }
    public void depart(Journey journey) throws IOException { start(journey, 3); }
    // Stop after a durable checkpoint so a restart can be demonstrated live.
    public void start(Journey j, int steps) throws IOException {
        if (steps < 1 || steps > 3) throw new AirportException("Steps must be 1-3");
        if (j.passenger() == null || j.passenger().isBlank()) throw new AirportException("Passenger name is required");
        chains.create(j); resume(j.id(), steps);
    }
    public void resume(String id) throws IOException { resume(id, 3); }
    private void resume(String id, int steps) throws IOException {
        var stage = chains.stage(id);
        if (stage == COMPENSATING) { compensate(id); return; }
        if (stage == COMPLETED || stage == COMPENSATED) return;
        Journey j = chains.load(id);
        try {
            if (chains.stage(id) == PENDING_A) {
                kiosk.issue(id, j.passenger()); chains.stage(id, PENDING_B);
                if (--steps == 0) return;
            }
            if (chains.stage(id) == PENDING_B) {
                baggage.screen(id, j.weight(), j.prohibited()); chains.stage(id, PENDING_C);
                if (--steps == 0) return;
            }
            gate.clear(id, j.passportValid(), j.customsDeclared()); chains.stage(id, COMPLETED);
        } catch (AirportException failure) {
            compensate(id); throw failure;
        }
    }
    public void compensate(String id) throws IOException {
        if (chains.stage(id) == COMPENSATED) return;
        chains.stage(id, COMPENSATING);
        // Every cancel reads actual durable state; safe after partial rollback and restart.
        gate.cancel(id); baggage.cancel(id); kiosk.cancel(id);
        chains.stage(id, COMPENSATED);
    }
}
