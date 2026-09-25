package dtm.stools.component.panels.editor.sheet.io.ooxml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class SheetPackage {
    public record Limits(long compressedBytes, long expandedBytes, long partBytes, int parts) {
        public static final Limits DEFAULT = new Limits(256L * 1024 * 1024, 1024L * 1024 * 1024, 512L * 1024 * 1024, 10_000);

        public Limits {
            if (compressedBytes <= 0 || expandedBytes <= 0 || partBytes <= 0 || parts <= 0) throw new IllegalArgumentException("Invalid limits");
        }
    }

    private final Map<String, byte[]> parts;

    public SheetPackage(Map<String, byte[]> parts) {
        Map<String, byte[]> copy = new LinkedHashMap<>();
        parts.forEach((name, data) -> { validateName(name); copy.put(name, data); });
        this.parts = Collections.unmodifiableMap(copy);
    }

    public Set<String> names() { return parts.keySet(); }
    public boolean contains(String name) { return parts.containsKey(name); }
    public Map<String, byte[]> parts() { return parts; }

    public byte[] part(String name) throws IOException {
        byte[] data = parts.get(name);
        if (data == null) throw new IOException("Parte ausente no pacote: " + name);
        return data;
    }

    public byte[] partOrNull(String name) { return parts.get(name); }

    public static byte[] readBounded(InputStream input, long limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[16384];
        long total = 0;
        int n;
        while ((n = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Cancelado");
            total += n;
            if (total > limit) throw new IOException("O arquivo excede o limite de tamanho configurado.");
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    public static SheetPackage read(byte[] bytes, Limits limits) throws IOException {
        if (bytes.length > limits.compressedBytes()) throw new IOException("Pacote compactado grande demais.");
        Map<String, byte[]> parts = new LinkedHashMap<>();
        long expanded = 0;
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > limits.parts()) throw new IOException("Pacote com entradas demais.");
                String name = entry.getName();
                if (entry.isDirectory()) continue;
                try { validateName(name); } catch (IllegalArgumentException e) { throw new IOException("Entrada ZIP inválida: " + name, e); }
                if (parts.containsKey(name)) throw new IOException("Entrada ZIP duplicada: " + name);
                byte[] data = readBounded(zip, Math.min(limits.partBytes(), limits.expandedBytes() - expanded));
                expanded += data.length;
                parts.put(name, data);
            }
        } catch (java.util.zip.ZipException e) {
            throw new IOException("Arquivo compactado inválido.", e);
        }
        return new SheetPackage(parts);
    }

    public void write(OutputStream output) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8);
        for (Map.Entry<String, byte[]> part : parts.entrySet()) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Cancelado");
            ZipEntry entry = new ZipEntry(part.getKey());
            entry.setTime(315532800000L);
            if (part.getKey().equals("mimetype")) {
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(part.getValue().length);
                CRC32 crc = new CRC32();
                crc.update(part.getValue());
                entry.setCrc(crc.getValue());
            }
            zip.putNextEntry(entry);
            zip.write(part.getValue());
            zip.closeEntry();
        }
        zip.finish();
        zip.flush();
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out);
        return out.toByteArray();
    }

    public static void validateName(String name) {
        if (name == null || name.isBlank() || name.startsWith("/") || name.contains("\\") || name.contains(":") || name.indexOf('\0') >= 0)
            throw new IllegalArgumentException("Nome de parte inválido");
        for (String segment : name.split("/", -1)) if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) throw new IllegalArgumentException("Caminho de parte inválido");
    }

    public static String resolve(String basePart, String target) {
        if (target.startsWith("/")) return target.substring(1);
        String dir = basePart.contains("/") ? basePart.substring(0, basePart.lastIndexOf('/') + 1) : "";
        java.util.Deque<String> stack = new java.util.ArrayDeque<>();
        for (String s : (dir + target).split("/")) {
            if (s.isEmpty() || s.equals(".")) continue;
            if (s.equals("..")) { if (!stack.isEmpty()) stack.removeLast(); continue; }
            stack.addLast(s);
        }
        return String.join("/", stack);
    }

    public static String relsPath(String part) {
        int slash = part.lastIndexOf('/');
        return (slash < 0 ? "" : part.substring(0, slash + 1)) + "_rels/" + part.substring(slash + 1) + ".rels";
    }
}
