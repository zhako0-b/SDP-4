package airport.processing;
import airport.common.AirportException;
public class SecurityScreening {
    public void validate(int weight, boolean prohibited) {
        if (prohibited) throw new AirportException("Security rejected prohibited baggage");
        if (weight < 0 || weight > 23) throw new AirportException("Baggage weight must be 0-23 kg");
    }
}
