package dtm.stools.component.panels.editor.word.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record WordListDefinition(String id, String abstractId, List<WordListLevel> levels, boolean restart) {
    public WordListDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(abstractId);
        levels = List.copyOf(levels);
        if (id.isBlank() || abstractId.isBlank() || levels.isEmpty() || levels.size() > 9) throw new IllegalArgumentException("Invalid list definition");
    }
    public WordListLevel level(int index) { return levels.get(Math.min(index,levels.size()-1)); }
    public boolean bullet() { return levels.getFirst().format() == WordListLevel.Format.BULLET; }
    public WordListDefinition restartedAs(String newId, int start) {
        List<WordListLevel> copy = new ArrayList<>(levels); copy.set(0,copy.getFirst().withStart(start));
        return new WordListDefinition(newId,abstractId,copy,true);
    }
    public static WordListDefinition bullets(String id) {
        String[] symbols = {"•","◦","▪"};
        List<WordListLevel> levels = new ArrayList<>();
        for (int i = 0; i < 9; i++) levels.add(new WordListLevel(WordListLevel.Format.BULLET,symbols[i%3],1,36 + i*18,18));
        return new WordListDefinition(id,"a" + id,levels,false);
    }
    public static WordListDefinition numbered(String id) {
        WordListLevel.Format[] formats = {WordListLevel.Format.DECIMAL,WordListLevel.Format.LOWER_LETTER,WordListLevel.Format.LOWER_ROMAN};
        List<WordListLevel> levels = new ArrayList<>();
        for (int i = 0; i < 9; i++) levels.add(new WordListLevel(formats[i%3],"%" + (i+1) + (i%3 == 1 ? ")" : "."),1,36 + i*18,18));
        return new WordListDefinition(id,"a" + id,levels,false);
    }
}
