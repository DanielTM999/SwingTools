package dtm.stools.component.panels.editor.word.io.ooxml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public final class OpcPackage {
    public record Limits(int compressedBytes,int expandedBytes,int partBytes,int parts) {
        public static final Limits DEFAULT=new Limits(32*1024*1024,128*1024*1024,32*1024*1024,2048);
        public Limits { if(compressedBytes<=0 || expandedBytes<=0 || partBytes<=0 || parts<=0) throw new IllegalArgumentException(); }
    }
    private final Map<String,byte[]> parts;
    public OpcPackage(Map<String,byte[]> parts) {
        Map<String,byte[]> copy=new LinkedHashMap<>();
        parts.forEach((name,data)-> { validateName(name); copy.put(name,data.clone()); });
        this.parts=Collections.unmodifiableMap(copy);
    }
    public Set<String> names() { return parts.keySet(); }
    public byte[] part(String name) throws IOException {
        byte[] data=parts.get(name); if(data==null) throw new IOException("Missing OPC part: "+name); return data.clone();
    }
    public boolean contains(String name) { return parts.containsKey(name); }
    public OpcPackage withPart(String name,byte[] data) { Map<String,byte[]> copy=new LinkedHashMap<>(parts); copy.put(name,data); return new OpcPackage(copy); }
    public static byte[] readBounded(InputStream input,int limit) throws IOException {
        ByteArrayOutputStream output=new ByteArrayOutputStream(); byte[] buffer=new byte[8192]; int n,total=0;
        while((n=input.read(buffer))!=-1) {
            if(Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Cancelled");
            total+=n; if(total>limit) throw new IOException("Input exceeds configured size limit"); output.write(buffer,0,n);
        }
        return output.toByteArray();
    }
    public static OpcPackage read(byte[] bytes,Limits limits) throws IOException {
        if(bytes.length>limits.compressedBytes) throw new IOException("Compressed package too large");
        Map<String,byte[]> parts=new LinkedHashMap<>(); int expanded=0,count=0;
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes),StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) {
                if(++count>limits.parts) throw new IOException("Too many ZIP entries");
                String name=entry.getName();
                if(entry.isDirectory()) { validateName(name.substring(0,name.length()-1)); continue; }
                try { validateName(name); } catch(IllegalArgumentException e) { throw new IOException("Invalid ZIP entry",e); }
                if(parts.containsKey(name)) throw new IOException("Duplicate ZIP entry: "+name);
                byte[] data=readBounded(zip,Math.min(limits.partBytes,limits.expandedBytes-expanded));
                expanded+=data.length; parts.put(name,data);
            }
        }
        if(!parts.containsKey("[Content_Types].xml") || !parts.containsKey("_rels/.rels")) throw new IOException("Not an OPC package");
        return new OpcPackage(parts);
    }
    public void write(OutputStream output) throws IOException {
        ZipOutputStream zip=new ZipOutputStream(output,StandardCharsets.UTF_8);
        for(var part:parts.entrySet()) {
            if(Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Cancelled");
            ZipEntry entry=new ZipEntry(part.getKey()); entry.setTime(0); zip.putNextEntry(entry); zip.write(part.getValue()); zip.closeEntry();
        }
        zip.finish(); zip.flush();
    }
    public static void validateName(String name) {
        if(name==null || name.isBlank() || name.startsWith("/") || name.contains("\\") || name.contains(":") || name.indexOf('\0')>=0)
            throw new IllegalArgumentException("Invalid part name");
        for(String segment:name.split("/",-1)) if(segment.isEmpty() || segment.equals(".") || segment.equals("..")) throw new IllegalArgumentException("Invalid part path");
    }
}
