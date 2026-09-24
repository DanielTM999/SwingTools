package dtm.stools.component.panels.editor.word.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class WordResource {
    private final String id;
    private final String contentType;
    private final byte[] data;

    private WordResource(String id, String contentType, byte[] data) { this.id = id; this.contentType = contentType; this.data = data; }

    public static WordResource of(byte[] data, String contentType) {
        Objects.requireNonNull(data); Objects.requireNonNull(contentType);
        if (data.length == 0 || data.length > 64 * 1024 * 1024) throw new IllegalArgumentException("Invalid resource size");
        if (!contentType.matches("[a-z]+/[a-z0-9.+-]+")) throw new IllegalArgumentException("Invalid content type");
        byte[] copy = data.clone();
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(copy)).substring(0,32);
            return new WordResource(hash,contentType,copy);
        } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    public String id() { return id; }
    public String contentType() { return contentType; }
    public byte[] data() { return data.clone(); }
    public int size() { return data.length; }
    public String extension() {
        return switch (contentType) {
            case "image/png" -> "png"; case "image/jpeg" -> "jpeg"; case "image/gif" -> "gif"; case "image/bmp" -> "bmp";
            case "image/svg+xml" -> "svg"; case "image/tiff" -> "tiff"; case "image/x-emf" -> "emf"; case "image/x-wmf" -> "wmf";
            default -> "bin";
        };
    }
    public static String contentTypeFor(String fileName) {
        String name = fileName.toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".bmp")) return "image/bmp";
        if (name.endsWith(".tif") || name.endsWith(".tiff")) return "image/tiff";
        if (name.endsWith(".emf")) return "image/x-emf";
        if (name.endsWith(".wmf")) return "image/x-wmf";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
    @Override public boolean equals(Object o) { return o instanceof WordResource r && r.id.equals(id) && r.contentType.equals(contentType); }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return "WordResource[" + id + ", " + contentType + ", " + data.length + " bytes]"; }
}
