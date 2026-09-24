package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.provider.WordRecoveryStore;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class WordFileRecoveryStore implements WordRecoveryStore {
    private final Path directory;

    public WordFileRecoveryStore(Path directory) { this.directory = Objects.requireNonNull(directory).toAbsolutePath().normalize(); }

    public Path directory() { return directory; }
    private Path file(String key) {
        if (key == null || !key.matches("[A-Za-z0-9._-]{1,120}") || key.contains("..")) throw new IllegalArgumentException("Invalid recovery key");
        return directory.resolve(key + ".docx");
    }
    @Override public void save(String key, byte[] content) throws IOException {
        Files.createDirectories(directory);
        Path target = file(key), temporary = Files.createTempFile(directory,".recovery-",".tmp");
        try {
            Files.write(temporary,content);
            try { Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    @Override public Optional<byte[]> load(String key) throws IOException {
        Path f = file(key);
        return Files.isRegularFile(f) ? Optional.of(Files.readAllBytes(f)) : Optional.empty();
    }
    @Override public void delete(String key) throws IOException { Files.deleteIfExists(file(key)); }
    @Override public List<Entry> list() throws IOException {
        if (!Files.isDirectory(directory)) return List.of();
        List<Entry> result = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path f : files.filter(p -> p.getFileName().toString().endsWith(".docx")).toList()) {
                String name = f.getFileName().toString();
                result.add(new Entry(name.substring(0,name.length()-5),Files.getLastModifiedTime(f).toInstant(),Files.size(f)));
            }
        }
        result.sort(Comparator.comparing(Entry::savedAt).reversed());
        return result;
    }
}
