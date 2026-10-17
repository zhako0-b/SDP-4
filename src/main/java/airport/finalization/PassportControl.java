package airport.finalization;
import airport.common.AirportException;
public class PassportControl {
    public void validate(boolean valid) {
        if (!valid) throw new AirportException("Passport is invalid");
    }
}
