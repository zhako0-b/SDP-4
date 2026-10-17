package airport.facade;
import airport.intake.*;
import airport.processing.*;
import airport.finalization.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
public class AdminFacade {
    private final Path root;
    public AdminFacade(Path root) { this.root = root; }
    public List<String> ids() throws IOException { return new ChainRepository(root).ids(); }
    public String inspect(String id) throws IOException {
        var stage = new ChainRepository(root).stage(id);
        var a = new Kiosk(root).find(id); var b = new BaggageHandler(root).find(id);
        var c = new GateController(root).find(id);
        return id + " | " + stage + " | A=" + (a == null ? "ABSENT" : a.status())
                + " | B=" + (b == null ? "ABSENT" : b.status()) + " | C=" + (c == null ? "ABSENT" : c.status());
    }
    public void compensate(String id) throws IOException { new DepartureFacade(root).compensate(id); }
}
