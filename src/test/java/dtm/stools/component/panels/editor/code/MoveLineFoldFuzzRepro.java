package dtm.stools.component.panels.editor.code;

import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MoveLineFoldFuzzRepro {

    public static void main(String[] args) throws Exception {
        int totalFailures = 0;
        long seeds = args.length > 0 ? Long.parseLong(args[0]) : 600;
        for (long seed = 0; seed < seeds; seed++) {
            try {
                if (!fuzz(seed)) totalFailures++;
            } catch (Throwable t) {
                totalFailures++;
                System.out.println("SEED " + seed + " EXCEPTION: " + t);
                t.printStackTrace(System.out);
            }
            if (totalFailures >= 8) break;
        }
        System.out.println(totalFailures == 0 ? "ALL OK" : ("FAILURES: " + totalFailures));
        System.exit(totalFailures == 0 ? 0 : 1);
    }

    static boolean fuzz(long seed) {
        Random rnd = new Random(seed);
        CodeEditorTextArea ed = new CodeEditorTextArea();
        ed.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        ed.setSize(800, 600);
        ed.setText(sampleText());
        ed.setFoldingEnabled(true);
        ed.addFoldRule(new dtm.stools.component.panels.editor.code.prototype.folding.FoldRule.Pair('{', '}'));

        List<String> log = new ArrayList<>();

        for (int i = 0; i < 150; i++) {
            int op = rnd.nextInt(16);
            if (op < 2) {
                int line = rnd.nextInt(Math.max(1, ed.buffer.lineCount()));
                log.add("toggleFold(" + line + ")");
                ed.toggleFold(line);
            } else if (op < 3) {

                List<Integer> visible = new ArrayList<>();
                for (int l = 0; l < ed.buffer.lineCount(); l++) {
                    if (!ed.isLineHidden(l)) visible.add(l);
                }
                int line = visible.get(rnd.nextInt(visible.size()));
                int col = rnd.nextInt(20);
                log.add("setCaretPosition(" + line + "," + col + ")");
                ed.setCaretPosition(line, col);
            } else if (op < 4) {

                int[] navKeys = {KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP,
                        KeyEvent.VK_DOWN, KeyEvent.VK_HOME, KeyEvent.VK_END};
                int key = navKeys[rnd.nextInt(navKeys.length)];
                int mods = rnd.nextInt(3) == 0 ? InputEvent.SHIFT_DOWN_MASK : 0;
                log.add("nav " + KeyEvent.getKeyText(key) + " mods=" + mods);
                pressKey(ed, key, mods);
            } else if (op < 6) {
                int foldedBefore = foldedCount(ed);
                char c = "abc {}xyz".charAt(rnd.nextInt(9));
                log.add("type '" + c + "'");
                typeChar(ed, c);
                if (foldedCount(ed) > foldedBefore) {
                    return fail(seed, log, "FOLD APPEARED apos digitar: " + describeFolds(ed));
                }
            } else if (op < 7) {
                int foldedBefore = foldedCount(ed);
                log.add("enter");
                pressKey(ed, KeyEvent.VK_ENTER, 0);
                if (foldedCount(ed) > foldedBefore) {
                    return fail(seed, log, "FOLD APPEARED apos enter: " + describeFolds(ed));
                }
            } else if (op < 8) {
                int foldedBefore = foldedCount(ed);
                log.add("backspace");
                pressKey(ed, KeyEvent.VK_BACK_SPACE, 0);
                if (foldedCount(ed) > foldedBefore) {
                    return fail(seed, log, "FOLD APPEARED apos backspace: " + describeFolds(ed));
                }
            } else if (op < 10) {
                boolean redo = rnd.nextBoolean();
                log.add(redo ? "ctrl+shift+Z" : "ctrl+Z");
                pressKey(ed, KeyEvent.VK_Z, redo
                        ? InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK
                        : InputEvent.CTRL_DOWN_MASK);
                if (ed.isLineHidden(ed.caretLine)) {
                    return fail(seed, log, "CARET ESCONDIDO apos undo/redo: line=" + ed.caretLine
                            + " folds=" + describeFolds(ed));
                }
            } else {
                List<String> beforeLines = allLines(ed);
                List<String> beforeVisible = visibleLines(ed);
                String opName;
                boolean duplicate;
                switch (op % 4) {
                    case 0 -> { opName = "moveLineUp"; duplicate = false; }
                    case 1 -> { opName = "moveLineDown"; duplicate = false; }
                    case 2 -> { opName = "duplicateLineUp"; duplicate = true; }
                    default -> { opName = "duplicateLineDown"; duplicate = true; }
                }
                log.add(opName + " caret=" + ed.caretLine + "," + ed.caretCol
                        + " folds=" + describeFolds(ed));
                switch (opName) {
                    case "moveLineUp" -> pressKey(ed, KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK);
                    case "moveLineDown" -> pressKey(ed, KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK);
                    case "duplicateLineUp" -> pressKey(ed, KeyEvent.VK_UP,
                            InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
                    default -> pressKey(ed, KeyEvent.VK_DOWN,
                            InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
                }

                List<String> afterLines = allLines(ed);
                List<String> afterVisible = visibleLines(ed);

                if (!duplicate) {
                    if (!sameMultiset(beforeLines, afterLines)) {
                        return fail(seed, log, "TEXT LOST/CHANGED em " + opName
                                + "\n--- before:\n" + String.join("\n", beforeLines)
                                + "\n--- after:\n" + String.join("\n", afterLines));
                    }
                    if (!containsMultiset(afterVisible, beforeVisible)) {
                        return fail(seed, log, "LINHA VISIVEL SUMIU em " + opName
                                + "\n--- folds after: " + describeFolds(ed)
                                + "\n--- visible before:\n" + String.join("\n", beforeVisible)
                                + "\n--- visible after:\n" + String.join("\n", afterVisible)
                                + "\n--- full text after:\n" + String.join("\n", afterLines));
                    }
                } else {
                    if (!containsMultiset(afterLines, beforeLines)) {
                        return fail(seed, log, "TEXT LOST em " + opName
                                + "\n--- before:\n" + String.join("\n", beforeLines)
                                + "\n--- after:\n" + String.join("\n", afterLines));
                    }
                    if (!containsMultiset(afterVisible, beforeVisible)) {
                        return fail(seed, log, "LINHA VISIVEL SUMIU em " + opName
                                + "\n--- folds after: " + describeFolds(ed)
                                + "\n--- visible before:\n" + String.join("\n", beforeVisible)
                                + "\n--- visible after:\n" + String.join("\n", afterVisible)
                                + "\n--- full text after:\n" + String.join("\n", afterLines));
                    }
                }

                if (ed.caretLine < 0 || ed.caretLine >= ed.buffer.lineCount()) {
                    return fail(seed, log, "CARET FORA: line=" + ed.caretLine);
                }
                if (ed.isLineHidden(ed.caretLine)) {
                    return fail(seed, log, "CARET ESCONDIDO em " + opName + ": line=" + ed.caretLine
                            + " folds=" + describeFolds(ed));
                }
                int len = ed.buffer.lineAt(ed.caretLine).length();
                if (ed.caretCol < 0 || ed.caretCol > len) {
                    return fail(seed, log, "CARET COL invalida: " + ed.caretCol + " len=" + len);
                }
            }
        }
        return true;
    }

    static List<String> allLines(CodeEditorTextArea ed) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < ed.buffer.lineCount(); i++) out.add(ed.buffer.lineAt(i));
        return out;
    }

    static List<String> visibleLines(CodeEditorTextArea ed) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < ed.buffer.lineCount(); i++) {
            if (!ed.isLineHidden(i)) out.add(ed.buffer.lineAt(i));
        }
        return out;
    }

    static boolean sameMultiset(List<String> a, List<String> b) {
        List<String> x = new ArrayList<>(a), y = new ArrayList<>(b);
        Collections.sort(x);
        Collections.sort(y);
        return x.equals(y);
    }

    static boolean containsMultiset(List<String> big, List<String> small) {
        List<String> rest = new ArrayList<>(big);
        for (String s : small) {
            if (!rest.remove(s)) return false;
        }
        return true;
    }

    static void pressKey(CodeEditorTextArea ed, int keyCode, int modifiers) {
        KeyEvent e = new KeyEvent(ed, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
        for (KeyListener kl : ed.getKeyListeners()) kl.keyPressed(e);
    }

    static void typeChar(CodeEditorTextArea ed, char c) {
        KeyEvent e = new KeyEvent(ed, KeyEvent.KEY_TYPED, System.currentTimeMillis(),
                0, KeyEvent.VK_UNDEFINED, c);
        for (KeyListener kl : ed.getKeyListeners()) kl.keyTyped(e);
    }

    static int foldedCount(CodeEditorTextArea ed) {
        java.util.Set<String> spans = new java.util.HashSet<>();
        for (var r : ed.foldRegions) {
            if (r.folded()) spans.add(r.startLine() + "-" + r.endLine());
        }
        return spans.size();
    }

    static String describeFolds(CodeEditorTextArea ed) {
        StringBuilder sb = new StringBuilder();
        for (var r : ed.foldRegions) {
            sb.append("[").append(r.startLine()).append("-").append(r.endLine())
              .append(r.folded() ? " FOLDED" : "").append("] ");
        }
        return sb.toString();
    }

    static boolean fail(long seed, List<String> log, String message) {
        System.out.println("==== SEED " + seed + " FAILED ====");
        int from = Math.max(0, log.size() - 15);
        for (int i = from; i < log.size(); i++) {
            System.out.println("  op[" + i + "] " + log.get(i));
        }
        System.out.println(message);
        return false;
    }

    static String sampleText() {
        return """
                public class Demo {
                    private int value;

                    public void run() {
                        int total = 0;
                        for (int i = 0; i < 10; i++) {
                            total += i;
                        }
                        System.out.println(total);
                    }

                    public int getValue() {
                        return value;
                    }
                }
                """;
    }
}
