package airport.intake;
import airport.common.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
public class Kiosk {
    private final Path file;
    public Kiosk(Path directory) { file = directory.resolve("intake.properties"); }
    public BoardingPass find(String id) throws IOException {
        String status = Disk.read(file).getProperty(Disk.key(id));
        return status == null ? null : new BoardingPass(BoardingPass.Status.valueOf(status));
    }
    public void issue(String id, String name) throws IOException {
        BoardingPass old = find(id);
        if (old != null) {
            if (old.status() == BoardingPass.Status.CANCELLED) throw new AirportException("Cancelled BoardingPass");
            return;
        }
        new BoardingPassIssuer().validate(name);
        
        BoardingPass entity = new BoardingPass(BoardingPass.Status.NEW);
        entity.issue(); save(id, entity);
    }
    public void cancel(String id) throws IOException {
        BoardingPass entity = find(id);
        if (entity == null || entity.status() == BoardingPass.Status.CANCELLED) return;
        entity.cancel(); save(id, entity);
    }
    private void save(String id, BoardingPass entity) throws IOException {
        Properties p = Disk.read(file);
        p.setProperty(Disk.key(id), entity.status().name()); Disk.write(file, p);
    }
}
