package airport.intake;
import airport.common.AirportException;
public class BoardingPassIssuer {
    public void validate(String name) {
        if (name == null || name.isBlank()) throw new AirportException("Passenger name is required");
    }
}
