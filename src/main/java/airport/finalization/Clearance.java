package airport.finalization;
import airport.common.AirportException;
public class Clearance {
    public enum Status { NEW, CLEARED, CANCELLED }
    private Status status;
    public Clearance(Status status) { this.status = status; }
    public Status status() { return status; }
    public void clear() {
        if (status != Status.NEW) throw new AirportException("Only NEW Clearance can be CLEARED");
        status = Status.CLEARED;
    }
    public void cancel() {
        if (status != Status.CLEARED) throw new AirportException("Only CLEARED Clearance can be cancelled");
        status = Status.CANCELLED;
    }
}
