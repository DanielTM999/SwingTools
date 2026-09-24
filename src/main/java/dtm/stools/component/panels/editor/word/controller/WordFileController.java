package dtm.stools.component.panels.editor.word.controller;

import dtm.stools.component.panels.editor.word.api.*;
import dtm.stools.component.panels.editor.word.config.WordServices;
import dtm.stools.component.panels.editor.word.io.WordImportResult;
import dtm.stools.component.panels.editor.word.model.WordDocument;
import dtm.stools.component.panels.editor.word.provider.WordExportProvider;
import dtm.stools.component.panels.editor.word.provider.WordRecoveryStore;
import dtm.stools.component.panels.editor.word.render.WordObjectRegistry;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class WordFileController implements AutoCloseable {
    private final WordSession session;
    private final WordServices services;
    private final WordTaskRunner runner;
    private final Supplier<WordObjectRegistry> registry;
    private final List<WordVersion> versions = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private WordImportResult origin;
    private Path currentFile;
    private byte[] diskHash;
    private long documentEpoch;
    private int maxVersions = 20;
    private WordRecoveryStore recoveryStore;
    private String recoveryKey;
    private Timer recoveryTimer;
    private long recoveredRevision = -1;
    private boolean recovering;

    public WordFileController(WordSession session, WordServices services, WordTaskRunner runner, Supplier<WordObjectRegistry> registry) {
        this.session = session; this.services = services; this.runner = runner; this.registry = registry;
    }
    public Optional<WordImportResult> origin() { return Optional.ofNullable(origin); }
    public Optional<Path> currentFile() { return Optional.ofNullable(currentFile); }
    public long epoch() { return documentEpoch; }
    public void addListener(Runnable listener) { listeners.add(listener); }
    private void fire() { for (Runnable r : List.copyOf(listeners)) r.run(); }
    public void replaced() { documentEpoch++; origin = null; currentFile = null; diskHash = null; fire(); }

    public WordTask<WordImportResult> open(Path path, boolean discardUnsaved) {
        if (session.isDirty() && !discardUnsaved) throw new IllegalStateException("Unsaved changes; explicitly choose whether to discard them");
        long revision = session.getRevision(), epoch = documentEpoch; Path absolute = path.toAbsolutePath().normalize();
        WordTask<WordImportResult> task = runner.task();
        task.attach(runner.submit(() -> { try {
            task.progress(10); WordImportResult imported;
            try (InputStream in = Files.newInputStream(absolute)) { imported = services.docx().read(in); }
            byte[] hash = sha256(imported.originalBytes()); task.progress(90);
            SwingUtilities.invokeLater(() -> task.commit(() -> {
                ensureOpen();
                if (revision != session.getRevision() || epoch != documentEpoch) throw new IOException("Document changed while opening");
                origin = imported; currentFile = absolute; diskHash = hash; documentEpoch++;
                session.load(imported.document()); versions.clear(); fire(); return imported;
            }));
        } catch (Throwable error) { runner.fail(task,error); } }));
        return task;
    }
    public WordTask<Path> save(Path path) {
        WordDocument snapshot = session.getDocument(); WordImportResult source = origin; long epoch = documentEpoch;
        Path target = path.toAbsolutePath().normalize(); byte[] expected = target.equals(currentFile) ? diskHash : null;
        WordObjectRegistry objects = registry.get();
        WordTask<Path> task = runner.task();
        task.attach(runner.submit(() -> { Path temporary = null; try {
            task.progress(10); Path parent = target.getParent(); if (parent == null || !Files.isDirectory(parent)) throw new IOException("Destination directory does not exist");
            temporary = Files.createTempFile(parent,".swingtools-word-",".tmp");
            try (OutputStream out = Files.newOutputStream(temporary)) { services.docx().write(snapshot,source,out,objects); }
            task.progress(80);
            if (task.isCancelled()) return;
            byte[] writtenHash = hashFile(temporary); Path completed = temporary;
            synchronized (task) {
                if (!task.beginCommit()) return;
                if (expected != null && (!Files.exists(target) || !Arrays.equals(expected,hashFile(target)))) throw new IOException("File was changed externally");
                Files.move(completed,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); temporary = null;
            }
            SwingUtilities.invokeLater(() -> task.commit(() -> {
                if (!runner.isClosed() && epoch == documentEpoch) {
                    currentFile = target; diskHash = writtenHash; session.markSaved(snapshot);
                    recordVersion(snapshot,target,"Salvo");
                    clearRecovery(); fire();
                }
                return target;
            }));
        } catch (Throwable error) { runner.fail(task,error); } finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) {} } }));
        return task;
    }
    public WordTask<Path> exportText(Path path, Function<WordDocument,String> producer) {
        WordDocument snapshot = session.getDocument(); WordTask<Path> task = runner.task(); Path target = path.toAbsolutePath().normalize();
        task.attach(runner.submit(() -> { Path temporary = null; try {
            String text = producer.apply(snapshot); task.progress(70);
            temporary = Files.createTempFile(target.getParent(),".swingtools-export-",".tmp"); Files.writeString(temporary,text,StandardCharsets.UTF_8);
            synchronized (task) { if (!task.beginCommit()) return; Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); temporary = null; }
            SwingUtilities.invokeLater(() -> task.complete(target));
        } catch (Throwable error) { runner.fail(task,error); } finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) {} } }));
        return task;
    }
    public WordTask<Path> export(Path path, WordExportProvider exporter) {
        WordDocument snapshot = session.getDocument(); WordTask<Path> task = runner.task(); Path target = path.toAbsolutePath().normalize();
        task.attach(runner.submit(() -> { Path temporary = null; try {
            var layout = services.layout().layout(snapshot); task.progress(40);
            temporary = Files.createTempFile(target.getParent(),".swingtools-export-",".tmp");
            try (OutputStream output = Files.newOutputStream(temporary)) { exporter.export(snapshot,layout,output); }
            task.progress(90);
            synchronized (task) { if (!task.beginCommit()) return; Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); temporary = null; }
            SwingUtilities.invokeLater(() -> task.complete(target));
        } catch (Throwable error) { runner.fail(task,error); } finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) {} } }));
        return task;
    }

    public List<WordVersion> versions() { return List.copyOf(versions); }
    public void setMaxVersions(int value) { if (value < 1) throw new IllegalArgumentException("At least one version"); maxVersions = value; while (versions.size() > maxVersions) versions.removeFirst(); }
    public WordVersion recordVersion(WordDocument snapshot, Path file, String label) {
        if (!versions.isEmpty() && versions.getLast().snapshot().equals(snapshot)) return versions.getLast();
        WordVersion version = new WordVersion(UUID.randomUUID().toString(),Instant.now(),file,label,snapshot);
        versions.add(version);
        while (versions.size() > maxVersions) versions.removeFirst();
        fire();
        return version;
    }
    public void restoreVersion(String id) {
        WordVersion version = versions.stream().filter(v -> v.id().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown version"));
        session.execute("Restaurar versão",d -> version.snapshot(),new WordSelection(0,0));
    }

    public void enableRecovery(WordRecoveryStore store, Duration interval, String key) {
        Objects.requireNonNull(store); Objects.requireNonNull(interval);
        if (interval.toMillis() < 200 || interval.toHours() > 24) throw new IllegalArgumentException("Recovery interval must be between 200 ms and 24 h");
        disableRecovery();
        recoveryStore = store; recoveryKey = key == null || key.isBlank() ? "documento" : key;
        recoveryTimer = new Timer((int)interval.toMillis(),e -> snapshotForRecovery());
        recoveryTimer.setRepeats(true); recoveryTimer.start();
    }
    public void disableRecovery() { if (recoveryTimer != null) recoveryTimer.stop(); recoveryTimer = null; recoveryStore = null; }
    public boolean isRecoveryEnabled() { return recoveryStore != null; }
    public WordTask<Boolean> snapshotForRecovery() {
        WordTask<Boolean> task = runner.task();
        WordRecoveryStore store = recoveryStore; String key = recoveryKey;
        if (store == null || recovering || !session.isDirty() || session.getRevision() == recoveredRevision) { task.complete(false); return task; }
        WordDocument snapshot = session.getDocument(); long revision = session.getRevision(); WordObjectRegistry objects = registry.get();
        recovering = true;
        task.attach(runner.submit(() -> {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream(); services.docx().write(snapshot,null,out,objects);
                store.save(key,out.toByteArray());
                SwingUtilities.invokeLater(() -> { recovering = false; recoveredRevision = revision; task.complete(true); });
            } catch (Throwable error) { SwingUtilities.invokeLater(() -> recovering = false); runner.fail(task,error); }
        }));
        return task;
    }
    public Optional<WordRecoveryStore.Entry> pendingRecovery(WordRecoveryStore store, String key) {
        try { return store.list().stream().filter(e -> e.key().equals(key)).findFirst(); } catch (IOException e) { return Optional.empty(); }
    }
    public WordTask<WordImportResult> recover(WordRecoveryStore store, String key) {
        WordTask<WordImportResult> task = runner.task();
        task.attach(runner.submit(() -> { try {
            byte[] bytes = store.load(key).orElseThrow(() -> new IOException("No recovery data for " + key));
            WordImportResult imported = services.docx().read(new ByteArrayInputStream(bytes));
            SwingUtilities.invokeLater(() -> task.commit(() -> {
                ensureOpen(); documentEpoch++; origin = null; currentFile = null; diskHash = null;
                session.load(imported.document()); session.markSaved(WordDocument.empty()); fire(); return imported;
            }));
        } catch (Throwable error) { runner.fail(task,error); } }));
        return task;
    }
    private void clearRecovery() {
        if (recoveryStore == null) return;
        WordRecoveryStore store = recoveryStore; String key = recoveryKey;
        runner.submit(() -> { try { store.delete(key); } catch (IOException ignored) {} });
        recoveredRevision = -1;
    }
    private void ensureOpen() { if (runner.isClosed()) throw new IllegalStateException("Editor is closed"); }
    static byte[] sha256(byte[] bytes) { try { return MessageDigest.getInstance("SHA-256").digest(bytes); } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); } }
    static byte[] hashFile(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) digest.update(buffer,0,n);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    @Override public void close() { disableRecovery(); listeners.clear(); }
}
