package dtm.stools.component.panels.editor.word.editing;

import dtm.stools.component.panels.editor.word.model.*;
import java.util.*;

public final class WordMailMerge {
    private WordMailMerge() {}

    public static List<WordDocument> documents(WordDocument template, List<Map<String,String>> records) {
        Objects.requireNonNull(records);
        List<WordDocument> result = new ArrayList<>();
        for (Map<String,String> record : records) result.add(WordTemplates.fill(template,record));
        return result;
    }
    public static WordDocument combined(WordDocument template, List<Map<String,String>> records) {
        List<WordDocument> filled = documents(template,records);
        if (filled.isEmpty()) throw new IllegalArgumentException("Nenhum registro para mesclar");
        List<WordBlock> blocks = new ArrayList<>();
        for (int i = 0; i < filled.size(); i++) {
            List<WordBlock> copy = new ArrayList<>();
            for (WordBlock b : filled.get(i).blocks()) copy.add(WordDocument.renew(b));
            if (i > 0 && copy.getFirst() instanceof WordParagraph p) copy.set(0,p.withStyle(p.style().withPageBreakBefore(true)));
            else if (i > 0) copy.addFirst(new WordParagraph(UUID.randomUUID(),List.of(),WordParagraphStyle.DEFAULT.withPageBreakBefore(true)));
            blocks.addAll(copy);
        }
        return new WordDocument(blocks,template.pageSettings(),template.parts());
    }
    public static List<Map<String,String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>(); StringBuilder cell = new StringBuilder(); boolean quoted = false;
        String text = WordDocument.normalize(csv);
        char separator = text.lines().findFirst().map(l -> l.chars().filter(c -> c == ';').count() > l.chars().filter(c -> c == ',').count() ? ';' : ',').orElse(',');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) { if (c == '"' && i+1 < text.length() && text.charAt(i+1) == '"') { cell.append('"'); i++; } else if (c == '"') quoted = false; else cell.append(c); continue; }
            if (c == '"') quoted = true;
            else if (c == separator) { row.add(cell.toString().strip()); cell.setLength(0); }
            else if (c == '\n') { row.add(cell.toString().strip()); cell.setLength(0); if (!(row.size() == 1 && row.getFirst().isEmpty())) rows.add(row); row = new ArrayList<>(); }
            else cell.append(c);
        }
        row.add(cell.toString().strip()); if (!(row.size() == 1 && row.getFirst().isEmpty())) rows.add(row);
        if (rows.size() < 2) throw new IllegalArgumentException("O CSV precisa de cabeçalho e ao menos um registro");
        List<String> header = rows.getFirst();
        List<Map<String,String>> result = new ArrayList<>();
        for (List<String> r : rows.subList(1,rows.size())) {
            Map<String,String> record = new LinkedHashMap<>();
            for (int k = 0; k < header.size(); k++) record.put(header.get(k),k < r.size() ? r.get(k) : "");
            result.add(record);
        }
        return result;
    }
}
