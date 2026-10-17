package airport.common;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class Disk {
    private Disk() {}
    public static Properties read(Path path) throws IOException {
        Properties p = new Properties();
        if (Files.exists(path)) try (var in = Files.newInputStream(path)) { p.load(in); }
        return p;
    }
    public static void write(Path path, Properties p) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (var out = new FileOutputStream(temp.toFile())) {
            p.store(out, null); out.getFD().sync();
        }
        Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    public static String key(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{1,40}"))
            throw new AirportException("ID must contain 1-40 letters, digits, _ or -");
        return id;
    }
}
