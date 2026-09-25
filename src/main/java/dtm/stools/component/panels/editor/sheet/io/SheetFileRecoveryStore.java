package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.provider.SheetRecoveryStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class SheetFileRecoveryStore implements SheetRecoveryStore {
    private final Path directory;
    private final int keep;

    public SheetFileRecoveryStore(Path directory) { this(directory, 10); }

    public SheetFileRecoveryStore(Path directory, int keep) {
        this.directory = directory;
        this.keep = Math.max(1, keep);
    }

    @Override public String id() { return "sheet.recovery.files"; }

    private Path folder(String key) throws IOException {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            Path dir = directory.resolve(HexFormat.of().formatHex(hash, 0, 12));
            Files.createDirectories(dir);
            return dir;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    @Override
    public synchronized void save(String key, byte[] data) throws IOException {
        Path dir = folder(key);
        long now = System.currentTimeMillis();
        Path temp = dir.resolve(now + ".tmp");
        Files.write(temp, data);
        Files.move(temp, dir.resolve(now + ".xlsx"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        List<Path> files = list(dir);
        for (int i = keep; i < files.size(); i++) Files.deleteIfExists(files.get(i));
    }

    private static List<Path> list(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".xlsx")).sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed()).toList();
        }
    }

    private static Snapshot snapshot(String key, Path p) throws IOException {
        String name = p.getFileName().toString();
        long millis = Long.parseLong(name.substring(0, name.length() - 5));
        return new Snapshot(name, key, Instant.ofEpochMilli(millis), Files.readAllBytes(p));
    }

    @Override
    public synchronized Optional<Snapshot> latest(String key) throws IOException {
        List<Path> files = list(folder(key));
        return files.isEmpty() ? Optional.empty() : Optional.of(snapshot(key, files.getFirst()));
    }

    @Override
    public synchronized List<Snapshot> history(String key) throws IOException {
        List<Snapshot> list = new ArrayList<>();
        for (Path p : list(folder(key))) list.add(snapshot(key, p));
        return list;
    }

    @Override
    public synchronized void clear(String key) throws IOException {
        for (Path p : list(folder(key))) Files.deleteIfExists(p);
    }
}
