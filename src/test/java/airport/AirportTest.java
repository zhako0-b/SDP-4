package airport;
import airport.common.*;
import airport.intake.*;
import airport.processing.*;
import airport.finalization.*;
import airport.facade.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static airport.facade.ChainRepository.Stage.*;
class AirportTest {
    @TempDir Path root;
    Journey passengerReadyToDepart(String id) { return new Journey(id, "Ali", 12, false, true, true); }
    DepartureFacade airport() { return new DepartureFacade(root); }
    ChainRepository chains() { return new ChainRepository(root); }
    void passengerHasState(String id, String a, String b, String c) throws IOException {
        assertEquals(id + " | " + chains().stage(id) + " | A=" + a + " | B=" + b + " | C=" + c,
                new AdminFacade(root).inspect(id));
    }
    @Test void passengerCompletesDeparture() throws Exception {
        airport().depart(passengerReadyToDepart("ok"));
        assertEquals(COMPLETED, chains().stage("ok"));
        passengerHasState("ok", "ISSUED", "SCREENED", "CLEARED");
        airport().resume("ok");
        assertEquals(java.util.List.of("ok"), new AdminFacade(root).ids());
    }
    @Test void securityFailureCompensatesOnlyIssuedPass() throws Exception {
        assertThrows(AirportException.class, () -> airport().depart(new Journey("b", "Ali", 12, true, true, true)));
        assertEquals(COMPENSATED, chains().stage("b"));
        passengerHasState("b", "CANCELLED", "ABSENT", "ABSENT");
        airport().resume("b"); airport().compensate("b");
    }
    @Test void passportFailureCompensatesBaggageThenPass() throws Exception {
        assertThrows(AirportException.class, () -> airport().depart(new Journey("c", "Ali", 12, false, false, true)));
        assertEquals(COMPENSATED, chains().stage("c"));
        passengerHasState("c", "CANCELLED", "CANCELLED", "ABSENT");
    }
    @Test void missingCustomsDeclarationAlsoCompensates() throws Exception {
        assertThrows(AirportException.class, () -> airport().depart(new Journey("c", "Ali", 12, false, true, false)));
        passengerHasState("c", "CANCELLED", "CANCELLED", "ABSENT");
    }
    @Test void restartAfterAReadsProgressAndCompletes() throws Exception {
        airport().start(passengerReadyToDepart("restart"), 1);
        assertEquals(PENDING_B, chains().stage("restart"));
        passengerHasState("restart", "ISSUED", "ABSENT", "ABSENT");
        new DepartureFacade(root).resume("restart");
        passengerHasState("restart", "ISSUED", "SCREENED", "CLEARED");
    }
    @Test void restartAfterBStillCompensatesBothOnFailure() throws Exception {
        airport().start(new Journey("r", "Ali", 10, false, false, true), 2);
        assertEquals(PENDING_C, chains().stage("r"));
        assertThrows(AirportException.class, () -> new DepartureFacade(root).resume("r"));
        passengerHasState("r", "CANCELLED", "CANCELLED", "ABSENT");
    }
    @Test void restartBetweenSubsystemCommitAndProgressWriteIsIdempotent() throws Exception {
        Journey j = passengerReadyToDepart("gap");
        chains().create(j);
        new Kiosk(root).issue("gap", "Ali");
        airport().start(passengerReadyToDepart("other"), 1);
        airport().resume("gap");
        passengerHasState("gap", "ISSUED", "SCREENED", "CLEARED");
        chains().stage("gap", PENDING_B); airport().resume("gap");
        passengerHasState("gap", "ISSUED", "SCREENED", "CLEARED");
    }
    @Test void interruptedCompensationPersistsBThenFinishesAAfterRestart() throws Exception {
        airport().start(passengerReadyToDepart("order"), 2);
        Path file = root.resolve("intake.properties"), backup = root.resolve("backup");
        Files.move(file, backup); Files.createDirectory(file);
        assertThrows(IOException.class, () -> airport().compensate("order"));
        assertEquals(COMPENSATING, chains().stage("order"));
        assertEquals(BaggageRecord.Status.CANCELLED, new BaggageHandler(root).find("order").status());
        Files.delete(file); Files.move(backup, file);
        assertEquals(BoardingPass.Status.ISSUED, new Kiosk(root).find("order").status());
        new DepartureFacade(root).resume("order");
        passengerHasState("order", "CANCELLED", "CANCELLED", "ABSENT");
    }
    @Test void manualCompensationCancelsCompletedJourney() throws Exception {
        airport().depart(passengerReadyToDepart("m"));
        new AdminFacade(root).compensate("m");
        new AdminFacade(root).compensate("m");
        passengerHasState("m", "CANCELLED", "CANCELLED", "CANCELLED");
    }
    @Test void invalidInputAndUnknownIdsAreRejected() throws Exception {
        assertThrows(AirportException.class, () -> airport().depart(new Journey("x", " ", 0, false, true, true)));
        assertThrows(AirportException.class, () -> airport().start(passengerReadyToDepart("x"), 0));
        assertThrows(AirportException.class, () -> airport().start(passengerReadyToDepart("x"), 4));
        assertThrows(AirportException.class, () -> airport().depart(passengerReadyToDepart("../escape")));
        assertThrows(AirportException.class, () -> airport().resume("unknown"));
        assertTrue(new AdminFacade(root).ids().isEmpty());
        airport().depart(passengerReadyToDepart("dup"));
        assertThrows(AirportException.class, () -> airport().depart(passengerReadyToDepart("dup")));
    }
    @Test void standaloneIntakeHasOwnRulesPersistenceAndTransitions() throws Exception {
        Kiosk k = new Kiosk(root);
        assertThrows(AirportException.class, () -> k.issue("i", ""));
        assertThrows(AirportException.class, () -> k.issue("i", null));
        k.issue("i", "Ali"); k.issue("i", "Ali");
        assertEquals(BoardingPass.Status.ISSUED, new Kiosk(root).find("i").status());
        k.cancel("i"); k.cancel("i"); k.cancel("absent");
        assertThrows(AirportException.class, () -> k.issue("i", "Ali"));
        BoardingPass p = new BoardingPass(BoardingPass.Status.NEW);
        assertThrows(AirportException.class, p::cancel); p.issue();
        assertThrows(AirportException.class, p::issue); p.cancel();
        assertThrows(AirportException.class, p::cancel);
    }
    @Test void standaloneProcessingHasOwnRulesPersistenceAndTransitions() throws Exception {
        BaggageHandler b = new BaggageHandler(root);
        assertThrows(AirportException.class, () -> b.screen("b", -1, false));
        assertThrows(AirportException.class, () -> b.screen("b", 24, false));
        b.screen("b", 23, false); b.screen("b", 23, false);
        assertEquals(BaggageRecord.Status.SCREENED, new BaggageHandler(root).find("b").status());
        b.cancel("b"); b.cancel("b"); b.cancel("absent");
        assertThrows(AirportException.class, () -> b.screen("b", 0, false));
        BaggageRecord r = new BaggageRecord(BaggageRecord.Status.NEW);
        assertThrows(AirportException.class, r::cancel); r.screen();
        assertThrows(AirportException.class, r::screen); r.cancel();
        assertThrows(AirportException.class, r::cancel);
    }
    @Test void standaloneFinalizationHasOwnRulesPersistenceAndTransitions() throws Exception {
        GateController g = new GateController(root);
        g.clear("c", true, true); g.clear("c", true, true);
        assertEquals(Clearance.Status.CLEARED, new GateController(root).find("c").status());
        g.cancel("c"); g.cancel("c"); g.cancel("absent");
        assertThrows(AirportException.class, () -> g.clear("c", true, true));
        Clearance c = new Clearance(Clearance.Status.NEW);
        assertThrows(AirportException.class, c::cancel); c.clear();
        assertThrows(AirportException.class, c::clear); c.cancel();
        assertThrows(AirportException.class, c::cancel);
    }
}
