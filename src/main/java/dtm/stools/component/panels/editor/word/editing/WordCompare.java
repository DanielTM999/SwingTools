package dtm.stools.component.panels.editor.word.editing;

import dtm.stools.component.panels.editor.word.model.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WordCompare {
    private static final Pattern TOKEN = Pattern.compile("\\s+|[\\p{L}\\p{N}_]+|.");
    private WordCompare() {}

    public static WordDocument compare(WordDocument original, WordDocument revised, String author) {
        WordRevision insert = WordRevisions.revision(WordRevision.Type.INSERT,author), delete = WordRevisions.revision(WordRevision.Type.DELETE,author);
        List<WordBlock> a = original.blocks(), b = revised.blocks();
        int[][] lcs = new int[a.size()+1][b.size()+1];
        for (int i = a.size()-1; i >= 0; i--) for (int j = b.size()-1; j >= 0; j--)
            lcs[i][j] = same(a.get(i),b.get(j)) ? lcs[i+1][j+1]+1 : Math.max(lcs[i+1][j],lcs[i][j+1]);
        List<WordBlock> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < a.size() || j < b.size()) {
            if (i < a.size() && j < b.size() && same(a.get(i),b.get(j))) {
                if (a.get(i) instanceof WordParagraph pa && b.get(j) instanceof WordParagraph pb && !pa.text().equals(pb.text())) result.add(words(pa,pb,insert,delete));
                else result.add(b.get(j));
                i++; j++;
            } else if (j < b.size() && (i >= a.size() || lcs[i][j+1] >= lcs[i+1][j])) {
                result.add(mark(WordDocument.renew(b.get(j)),insert)); j++;
            } else {
                result.add(mark(WordDocument.renew(a.get(i)),delete)); i++;
            }
        }
        WordParts parts = revised.parts().withResources(revised.resources().merge(original.resources()));
        return new WordDocument(result,revised.pageSettings(),parts);
    }
    private static boolean same(WordBlock x, WordBlock y) {
        if (x instanceof WordParagraph p && y instanceof WordParagraph q) {
            if (p.text().equals(q.text())) return true;
            return similarity(p.text(),q.text()) >= 0.5;
        }
        return x.getClass() == y.getClass() && x.plainText().equals(y.plainText());
    }
    private static double similarity(String x, String y) {
        Set<String> a = new HashSet<>(Arrays.asList(x.toLowerCase(Locale.ROOT).split("\\s+"))), b = new HashSet<>(Arrays.asList(y.toLowerCase(Locale.ROOT).split("\\s+")));
        if (a.isEmpty() && b.isEmpty()) return 1;
        Set<String> common = new HashSet<>(a); common.retainAll(b);
        return 2.0*common.size()/(a.size()+b.size());
    }
    private static WordBlock mark(WordBlock block, WordRevision revision) {
        if (block instanceof WordParagraph p) {
            List<WordInline> runs = new ArrayList<>();
            for (WordInline inline : p.runs()) runs.add(inline.withStyle(inline.style().withRevision(revision)));
            return p.withRuns(runs);
        }
        return block;
    }
    private record Token(String text, WordInline source) {}
    private static List<Token> tokens(WordParagraph p) {
        List<Token> result = new ArrayList<>();
        for (WordInline inline : p.runs()) {
            if (inline instanceof WordObjectRun) { result.add(new Token(inline.text(),inline)); continue; }
            Matcher m = TOKEN.matcher(inline.text());
            while (m.find()) result.add(new Token(m.group(),inline));
        }
        return result;
    }
    private static WordParagraph words(WordParagraph original, WordParagraph revised, WordRevision insert, WordRevision delete) {
        List<Token> a = tokens(original), b = tokens(revised);
        int[][] lcs = new int[a.size()+1][b.size()+1];
        for (int i = a.size()-1; i >= 0; i--) for (int j = b.size()-1; j >= 0; j--)
            lcs[i][j] = a.get(i).text().equals(b.get(j).text()) ? lcs[i+1][j+1]+1 : Math.max(lcs[i+1][j],lcs[i][j+1]);
        List<WordInline> runs = new ArrayList<>();
        int i = 0, j = 0;
        while (i < a.size() || j < b.size()) {
            if (i < a.size() && j < b.size() && a.get(i).text().equals(b.get(j).text())) { runs.add(piece(b.get(j),null)); i++; j++; }
            else if (i < a.size() && (j >= b.size() || lcs[i+1][j] >= lcs[i][j+1])) { runs.add(piece(a.get(i),delete)); i++; }
            else { runs.add(piece(b.get(j),insert)); j++; }
        }
        return revised.withRuns(runs);
    }
    private static WordInline piece(Token token, WordRevision revision) {
        WordInline source = token.source();
        WordInline base = source instanceof WordObjectRun o ? o.withObject(o.object().withId(WordIds.next())) : new WordRun(token.text(),source.style());
        return revision == null ? base : base.withStyle(base.style().withRevision(revision));
    }
}
