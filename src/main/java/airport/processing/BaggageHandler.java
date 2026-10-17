package airport.processing;
import airport.common.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
public class BaggageHandler {
    private final Path file;
    public BaggageHandler(Path directory) { file = directory.resolve("processing.properties"); }
    public BaggageRecord find(String id) throws IOException {
        String status = Disk.read(file).getProperty(Disk.key(id));
        return status == null ? null : new BaggageRecord(BaggageRecord.Status.valueOf(status));
    }
    public void screen(String id, int weight, boolean prohibited) throws IOException {
        BaggageRecord old = find(id);
        if (old != null) {
            if (old.status() == BaggageRecord.Status.CANCELLED) throw new AirportException("Cancelled BaggageRecord");
            return;
        }
        new SecurityScreening().validate(weight, prohibited);
        
        BaggageRecord entity = new BaggageRecord(BaggageRecord.Status.NEW);
        entity.screen(); save(id, entity);
    }
    public void cancel(String id) throws IOException {
        BaggageRecord entity = find(id);
        if (entity == null || entity.status() == BaggageRecord.Status.CANCELLED) return;
        entity.cancel(); save(id, entity);
    }
    private void save(String id, BaggageRecord entity) throws IOException {
        Properties p = Disk.read(file);
        p.setProperty(Disk.key(id), entity.status().name()); Disk.write(file, p);
    }
}
