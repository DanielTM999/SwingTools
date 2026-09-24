package dtm.stools.component.panels.editor.word.model;

import java.util.UUID;

public final class WordIds {
    private WordIds() {}
    public static String next() { return UUID.randomUUID().toString().replace("-","").substring(0,16); }
}
