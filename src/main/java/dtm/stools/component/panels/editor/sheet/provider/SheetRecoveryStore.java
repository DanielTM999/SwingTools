package dtm.stools.component.panels.editor.sheet.provider;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SheetRecoveryStore extends SheetProvider {
    record Snapshot(String id, String key, Instant created, byte[] data) {}

    void save(String key, byte[] data) throws IOException;
    Optional<Snapshot> latest(String key) throws IOException;
    List<Snapshot> history(String key) throws IOException;
    void clear(String key) throws IOException;
}
