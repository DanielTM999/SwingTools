package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.model.WordDocument;
import dtm.stools.component.panels.editor.word.model.WordHeaders;
import dtm.stools.component.panels.editor.word.model.WordInlineObject;
import dtm.stools.component.panels.editor.word.io.ooxml.OpcPackage;
import java.util.*;

public final class WordImportResult {
    public record OriginalObject(WordInlineObject parsed, String xml, String part) {}
    private final WordDocument document;
    private final List<String> diagnostics,blocking;
    private final byte[] original;
    private final OpcPackage source;
    private final String mainPart,namespace;
    private final Map<String,String> rootNamespaces;
    private final String ignorable;
    private final Map<String,OriginalObject> originals;
    private final Map<WordHeaders.Kind,String> headerReferences;
    public WordImportResult(WordDocument document,List<String> diagnostics,byte[] original,OpcPackage source,String mainPart,String namespace) {
        this(document,diagnostics,List.of(),original,source,mainPart,namespace,Map.of(),"",Map.of(),Map.of());
    }
    public WordImportResult(WordDocument document,List<String> diagnostics,List<String> blocking,byte[] original,OpcPackage source,String mainPart,String namespace,
                            Map<String,String> rootNamespaces,String ignorable,Map<String,OriginalObject> originals,Map<WordHeaders.Kind,String> headerReferences) {
        this.document=Objects.requireNonNull(document); this.diagnostics=List.copyOf(diagnostics); this.blocking=List.copyOf(blocking); this.original=original.clone();
        this.source=source; this.mainPart=mainPart; this.namespace=namespace;
        this.rootNamespaces=Collections.unmodifiableMap(new LinkedHashMap<>(rootNamespaces)); this.ignorable=ignorable==null?"":ignorable;
        this.originals=Map.copyOf(originals); this.headerReferences=Map.copyOf(headerReferences);
    }
    public WordDocument document() { return document; }
    public List<String> diagnostics() { return diagnostics; }
    public List<String> blockingReasons() { return blocking; }
    public boolean isEditable() { return blocking.isEmpty(); }
    public byte[] originalBytes() { return original.clone(); }
    public OpcPackage source() { return source; }
    public String mainPart() { return mainPart; }
    public String namespace() { return namespace; }
    public Map<String,String> rootNamespaces() { return rootNamespaces; }
    public String ignorable() { return ignorable; }
    public Map<String,OriginalObject> originals() { return originals; }
    public Map<WordHeaders.Kind,String> headerReferences() { return headerReferences; }
}
