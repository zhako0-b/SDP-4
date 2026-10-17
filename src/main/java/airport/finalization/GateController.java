package airport.finalization;
import airport.common.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
public class GateController {
    private final Path file;
    public GateController(Path directory) { file = directory.resolve("finalization.properties"); }
    public Clearance find(String id) throws IOException {
        String status = Disk.read(file).getProperty(Disk.key(id));
        return status == null ? null : new Clearance(Clearance.Status.valueOf(status));
    }
    public void clear(String id, boolean valid, boolean declared) throws IOException {
        Clearance old = find(id);
        if (old != null) {
            if (old.status() == Clearance.Status.CANCELLED) throw new AirportException("Cancelled Clearance");
            return;
        }
        new PassportControl().validate(valid);
        new CustomsDeclaration().validate(declared);
        Clearance entity = new Clearance(Clearance.Status.NEW);
        entity.clear(); save(id, entity);
    }
    public void cancel(String id) throws IOException {
        Clearance entity = find(id);
        if (entity == null || entity.status() == Clearance.Status.CANCELLED) return;
        entity.cancel(); save(id, entity);
    }
    private void save(String id, Clearance entity) throws IOException {
        Properties p = Disk.read(file);
        p.setProperty(Disk.key(id), entity.status().name()); Disk.write(file, p);
    }
}
