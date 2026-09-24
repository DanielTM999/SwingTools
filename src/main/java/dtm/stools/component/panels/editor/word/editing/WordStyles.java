package dtm.stools.component.panels.editor.word.editing;

import dtm.stools.component.panels.editor.word.model.*;
import java.util.*;

public final class WordStyles {
    private WordStyles() {}

    public static WordDocument apply(WordDocument document, int start, int end, String styleId) {
        WordStyleSheet styles = document.styles();
        WordNamedStyle style = styles.get(styleId).orElseThrow(() -> new IllegalArgumentException("Estilo inexistente: " + styleId));
        if (style.type() == WordNamedStyle.Type.CHARACTER) {
            List<WordNamedStyle> chain = styles.chain(styleId);
            return document.format(start,end,s -> { WordTextStyle r = s; for (WordNamedStyle n : chain) r = n.properties().apply(r); return r; });
        }
        WordParagraphStyle resolved = styles.resolveParagraph(styleId);
        WordTextStyle text = styles.resolveText(styleId);
        return document.mapParagraphs(start,end,p -> restyle(p,resolved,text));
    }
    private static WordParagraph restyle(WordParagraph p, WordParagraphStyle resolved, WordTextStyle text) {
        WordParagraphStyle current = p.style();
        WordParagraphStyle next = resolved.withList(current.list()).withTabs(current.tabs()).withExtras(current.extras()).withPageBreakBefore(current.pageBreakBefore());
        if (current.list() != null) next = next.withIndents(current.leftIndent(),current.rightIndent(),current.firstLineIndent());
        List<WordInline> runs = new ArrayList<>();
        for (WordInline inline : p.runs()) {
            WordTextStyle s = inline.style();
            runs.add(inline.withStyle(text.withVerticalAlign(s.verticalAlign()).withLink(s.link()).withRevision(s.revision()).withComments(s.comments()).withExtras(s.extras())
                    .withUnderline(text.underline() || s.link() != null).withColor(s.link() != null ? s.color() : text.color())));
        }
        return p.withStyle(next).withRuns(runs);
    }
    public static WordDocument update(WordDocument document, WordNamedStyle style) {
        WordStyleSheet sheet = document.styles().with(style);
        WordDocument next = document.withParts(document.parts().withStyles(sheet));
        return next.mapAllParagraphs(p -> {
            String id = p.style().styleId();
            if (id == null || !sheet.inherits(id,style.id())) return p;
            return restyle(p,sheet.resolveParagraph(id),sheet.resolveText(id));
        });
    }
    public static WordNamedStyle fromParagraph(String id, String name, WordParagraph paragraph, String basedOn) {
        WordTextStyle t = paragraph.runs().isEmpty() ? WordTextStyle.DEFAULT : paragraph.runs().getFirst().style();
        WordParagraphStyle p = paragraph.style();
        WordStyleProperties props = new WordStyleProperties(t.family(),t.size(),t.bold(),t.italic(),t.underline(),t.color(),p.alignment(),p.before(),p.after(),p.lineSpacing(),
                p.leftIndent(),p.firstLineIndent(),p.headingLevel(),p.keepWithNext());
        return new WordNamedStyle(id,name,basedOn,props);
    }
}
