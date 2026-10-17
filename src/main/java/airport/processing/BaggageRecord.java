package airport.processing;
import airport.common.AirportException;
public class BaggageRecord {
    public enum Status { NEW, SCREENED, CANCELLED }
    private Status status;
    public BaggageRecord(Status status) { this.status = status; }
    public Status status() { return status; }
    public void screen() {
        if (status != Status.NEW) throw new AirportException("Only NEW BaggageRecord can be SCREENED");
        status = Status.SCREENED;
    }
    public void cancel() {
        if (status != Status.SCREENED) throw new AirportException("Only SCREENED BaggageRecord can be cancelled");
        status = Status.CANCELLED;
    }
}
