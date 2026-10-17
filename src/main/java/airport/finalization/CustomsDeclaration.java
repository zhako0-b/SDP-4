package airport.finalization;
import airport.common.AirportException;
public class CustomsDeclaration {
    public void validate(boolean declared) {
        if (!declared) throw new AirportException("Required customs declaration is missing");
    }
}
