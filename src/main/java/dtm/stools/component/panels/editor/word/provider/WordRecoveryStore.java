package dtm.stools.component.panels.editor.word.provider;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WordRecoveryStore {
    record Entry(String key, Instant savedAt, long size) {}
    void save(String key, byte[] content) throws IOException;
    Optional<byte[]> load(String key) throws IOException;
    void delete(String key) throws IOException;
    List<Entry> list() throws IOException;
}
