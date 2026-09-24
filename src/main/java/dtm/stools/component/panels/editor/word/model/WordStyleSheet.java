package dtm.stools.component.panels.editor.word.model;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class WordStyleSheet {
    public static final String NORMAL = "Normal";
    private static final WordStyleSheet DEFAULTS = createDefaults();
    private final Map<String,WordNamedStyle> styles;
    private final WordTextStyle defaultText;
    private final WordParagraphStyle defaultParagraph;

    public WordStyleSheet(Collection<WordNamedStyle> styles, WordTextStyle defaultText, WordParagraphStyle defaultParagraph) {
        Map<String,WordNamedStyle> map = new LinkedHashMap<>();
        for (WordNamedStyle s : styles) map.put(s.id(),s);
        this.styles = Collections.unmodifiableMap(map);
        this.defaultText = Objects.requireNonNull(defaultText);
        this.defaultParagraph = Objects.requireNonNull(defaultParagraph);
    }
    public static WordStyleSheet defaults() { return DEFAULTS; }
    private static WordStyleSheet createDefaults() {
        var a = WordParagraphStyle.Alignment.CENTER;
        List<WordNamedStyle> list = List.of(
                new WordNamedStyle(NORMAL,"Normal",null,WordStyleProperties.text("Arial",11f,false,false,0x111111).withParagraph(null,0f,8f,1.15f,0,null)),
                new WordNamedStyle("Title","Título",NORMAL,WordStyleProperties.text(null,28f,true,false,0x1F3864).withParagraph(null,0f,4f,1f,null,true)),
                new WordNamedStyle("Subtitle","Subtítulo",NORMAL,WordStyleProperties.text(null,15f,false,true,0x595959).withParagraph(null,0f,12f,null,null,null)),
                new WordNamedStyle("Heading1","Título 1",NORMAL,WordStyleProperties.text(null,20f,true,false,0x2F5496).withParagraph(null,18f,6f,null,1,true)),
                new WordNamedStyle("Heading2","Título 2",NORMAL,WordStyleProperties.text(null,16f,true,false,0x2F5496).withParagraph(null,12f,4f,null,2,true)),
                new WordNamedStyle("Heading3","Título 3",NORMAL,WordStyleProperties.text(null,13f,true,false,0x1F3763).withParagraph(null,10f,4f,null,3,true)),
                new WordNamedStyle("Quote","Citação",NORMAL,WordStyleProperties.text(null,null,false,true,0x404040).withParagraph(a,6f,10f,null,null,null).withIndents(36f,null)),
                new WordNamedStyle("ListParagraph","Parágrafo da lista",NORMAL,WordStyleProperties.NONE.withIndents(36f,null)),
                new WordNamedStyle("Caption","Legenda",NORMAL,WordStyleProperties.text(null,9f,false,true,0x44546A).withParagraph(null,0f,10f,null,null,null)),
                new WordNamedStyle("TOCHeading","Título do sumário","Heading1",WordStyleProperties.NONE),
                new WordNamedStyle("TOC1","Sumário 1",NORMAL,WordStyleProperties.NONE.withParagraph(null,null,4f,null,null,null)),
                new WordNamedStyle("TOC2","Sumário 2",NORMAL,WordStyleProperties.NONE.withParagraph(null,null,4f,null,null,null).withIndents(11f,null)),
                new WordNamedStyle("TOC3","Sumário 3",NORMAL,WordStyleProperties.NONE.withParagraph(null,null,4f,null,null,null).withIndents(22f,null)),
                new WordNamedStyle("FootnoteText","Texto de nota de rodapé",NORMAL,WordStyleProperties.text(null,9f,null,null,null).withParagraph(null,0f,0f,1f,null,null)),
                new WordNamedStyle("Header","Cabeçalho",NORMAL,WordStyleProperties.NONE.withParagraph(null,0f,0f,1f,null,null)),
                new WordNamedStyle("Footer","Rodapé",NORMAL,WordStyleProperties.NONE.withParagraph(null,0f,0f,1f,null,null)));
        return new WordStyleSheet(list,WordTextStyle.DEFAULT,WordParagraphStyle.DEFAULT);
    }
    public Collection<WordNamedStyle> styles() { return styles.values(); }
    public List<WordNamedStyle> paragraphStyles() { return styles.values().stream().filter(s -> s.type() == WordNamedStyle.Type.PARAGRAPH).toList(); }
    public Optional<WordNamedStyle> get(String id) { return Optional.ofNullable(id == null ? null : styles.get(id)); }
    public WordTextStyle defaultText() { return defaultText; }
    public WordParagraphStyle defaultParagraph() { return defaultParagraph; }
    public WordStyleSheet with(WordNamedStyle style) {
        Map<String,WordNamedStyle> copy = new LinkedHashMap<>(styles); copy.put(style.id(),style);
        WordStyleSheet next = new WordStyleSheet(copy.values(),defaultText,defaultParagraph);
        next.chain(style.id());
        return next;
    }
    public WordStyleSheet withDefaults(WordTextStyle text, WordParagraphStyle paragraph) { return new WordStyleSheet(styles.values(),text,paragraph); }
    public WordStyleSheet merge(WordStyleSheet missing) {
        Map<String,WordNamedStyle> copy = new LinkedHashMap<>(styles);
        for (WordNamedStyle s : missing.styles()) copy.putIfAbsent(s.id(),s);
        return new WordStyleSheet(copy.values(),defaultText,defaultParagraph);
    }
    public List<WordNamedStyle> chain(String id) {
        Deque<WordNamedStyle> result = new ArrayDeque<>(); Set<String> seen = new HashSet<>();
        String current = id;
        while (current != null && styles.containsKey(current)) {
            if (!seen.add(current) || seen.size() > 32) throw new IllegalArgumentException("Circular style inheritance: " + id);
            WordNamedStyle s = styles.get(current); result.addFirst(s); current = s.basedOn();
        }
        return List.copyOf(result);
    }
    public boolean inherits(String id, String ancestor) { return chain(id).stream().anyMatch(s -> s.id().equals(ancestor)); }
    public WordTextStyle resolveText(String styleId) {
        WordTextStyle result = defaultText;
        for (WordNamedStyle s : chain(styleId == null ? NORMAL : styleId)) result = s.properties().apply(result);
        return result;
    }
    public WordParagraphStyle resolveParagraph(String styleId) {
        WordParagraphStyle result = defaultParagraph;
        for (WordNamedStyle s : chain(styleId == null ? NORMAL : styleId)) result = s.properties().apply(result);
        return result.withStyleId(styleId);
    }
    public String nextStyle(String styleId) { return get(styleId).map(WordNamedStyle::next).orElse(styleId != null && styleId.startsWith("Heading") || "Title".equals(styleId) || "Subtitle".equals(styleId) ? NORMAL : styleId); }
    @Override public boolean equals(Object o) { return o instanceof WordStyleSheet s && s.styles.equals(styles) && s.defaultText.equals(defaultText) && s.defaultParagraph.equals(defaultParagraph); }
    @Override public int hashCode() { return Objects.hash(styles,defaultText,defaultParagraph); }
}
