package airport.intake;
import airport.common.AirportException;
public class BoardingPass {
    public enum Status { NEW, ISSUED, CANCELLED }
    private Status status;
    public BoardingPass(Status status) { this.status = status; }
    public Status status() { return status; }
    public void issue() {
        if (status != Status.NEW) throw new AirportException("Only NEW BoardingPass can be ISSUED");
        status = Status.ISSUED;
    }
    public void cancel() {
        if (status != Status.ISSUED) throw new AirportException("Only ISSUED BoardingPass can be cancelled");
        status = Status.CANCELLED;
    }
}
