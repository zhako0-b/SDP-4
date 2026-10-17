package airport.facade;
import airport.common.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
public class ChainRepository {
    public enum Stage { PENDING_A, PENDING_B, PENDING_C, COMPLETED, COMPENSATING, COMPENSATED }
    private final Path directory;
    public ChainRepository(Path root) { directory = root.resolve("chains"); }
    private Path file(String id) { return directory.resolve(Disk.key(id) + ".properties"); }
    public void create(Journey j) throws IOException {
        if (Files.exists(file(j.id()))) throw new AirportException("Journey ID already exists");
        Properties p = new Properties();
        p.setProperty("passenger", j.passenger()); p.setProperty("weight", "" + j.weight());
        p.setProperty("prohibited", "" + j.prohibited());
        p.setProperty("passport", "" + j.passportValid()); p.setProperty("customs", "" + j.customsDeclared());
        p.setProperty("stage", Stage.PENDING_A.name()); Disk.write(file(j.id()), p);
    }
    private Properties required(String id) throws IOException {
        Properties p = Disk.read(file(id));
        if (p.isEmpty()) throw new AirportException("Journey not found: " + id);
        return p;
    }
    public Journey load(String id) throws IOException {
        Properties p = required(id);
        return new Journey(id, p.getProperty("passenger"), Integer.parseInt(p.getProperty("weight")),
                Boolean.parseBoolean(p.getProperty("prohibited")), Boolean.parseBoolean(p.getProperty("passport")),
                Boolean.parseBoolean(p.getProperty("customs")));
    }
    public Stage stage(String id) throws IOException { return Stage.valueOf(required(id).getProperty("stage")); }
    public void stage(String id, Stage stage) throws IOException {
        Properties p = required(id); p.setProperty("stage", stage.name()); Disk.write(file(id), p);
    }
    public List<String> ids() throws IOException {
        if (!Files.exists(directory)) return List.of();
        try (var paths = Files.list(directory)) {
            return paths.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".properties"))
                    .map(n -> n.substring(0, n.length()-11)).sorted().toList();
        }
    }
}
