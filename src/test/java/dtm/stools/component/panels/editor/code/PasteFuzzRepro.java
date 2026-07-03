package dtm.stools.component.panels.editor.code;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Harness de reproducao: dispara eventos de teclado reais no CodeEditorTextArea
 * (digitacao, navegacao, selecao, Ctrl+C/V/X, undo/redo) e valida invariantes
 * apos cada operacao para capturar o bug de "colar come o texto de baixo".
 */
public class PasteFuzzRepro {

    static final int CTRL = InputEvent.CTRL_DOWN_MASK;
    static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;

    public static void main(String[] args) throws Exception {
        int totalFailures = 0;
        long seeds = args.length > 0 ? Long.parseLong(args[0]) : 400;
        for (long seed = 0; seed < seeds; seed++) {
            try {
                if (!fuzz(seed)) totalFailures++;
            } catch (Throwable t) {
                totalFailures++;
                System.out.println("SEED " + seed + " EXCEPTION: " + t);
                t.printStackTrace(System.out);
            }
            if (totalFailures >= 5) break;
        }
        System.out.println(totalFailures == 0 ? "ALL OK" : ("FAILURES: " + totalFailures));
        System.exit(totalFailures == 0 ? 0 : 1);
    }

    static boolean fuzz(long seed) throws Exception {
        Random rnd = new Random(seed);
        CodeEditorTextArea ed = new CodeEditorTextArea();
        ed.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        ed.setSize(800, 600);
        ed.setText(sampleText());
        ed.setFoldingEnabled(true);
        ed.addFoldRule(new dtm.stools.component.panels.editor.code.prototype.folding.FoldRule.Pair('{', '}'));

        List<String> log = new ArrayList<>();

        for (int i = 0; i < 150; i++) {
            int op = rnd.nextInt(106);
            int foldedBefore = foldedCount(ed);
            boolean foldOp = op >= 100;
            if (op < 25) {
                char c = "abcdefghij xyz0123".charAt(rnd.nextInt(18));
                log.add("type '" + c + "'");
                typeChar(ed, c, 0);
            } else if (op < 32) {
                char c = "{}()[]\"'".charAt(rnd.nextInt(8));
                log.add("type '" + c + "'");
                typeChar(ed, c, 0);
            } else if (op < 40) {
                log.add("enter");
                pressKey(ed, KeyEvent.VK_ENTER, 0);
            } else if (op < 50) {
                log.add("backspace");
                pressKey(ed, KeyEvent.VK_BACK_SPACE, 0);
            } else if (op < 55) {
                log.add("delete");
                pressKey(ed, KeyEvent.VK_DELETE, 0);
            } else if (op < 58) {
                log.add("tab");
                pressKey(ed, KeyEvent.VK_TAB, 0);
            } else if (op < 72) {
                int[] navKeys = {KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP,
                        KeyEvent.VK_DOWN, KeyEvent.VK_HOME, KeyEvent.VK_END};
                int key = navKeys[rnd.nextInt(navKeys.length)];
                int mods = 0;
                if (rnd.nextInt(3) == 0) mods |= SHIFT;
                if (rnd.nextInt(5) == 0) mods |= CTRL;
                log.add("nav " + KeyEvent.getKeyText(key) + " mods=" + mods);
                pressKey(ed, key, mods);
            } else if (op < 74) {
                log.add("ctrl+A");
                pressKey(ed, KeyEvent.VK_A, CTRL);
            } else if (op < 78) {
                log.add("ctrl+C");
                pressKey(ed, KeyEvent.VK_C, CTRL);
            } else if (op < 81) {
                log.add("ctrl+X");
                pressKey(ed, KeyEvent.VK_X, CTRL);
            } else if (op < 91) {
                String clip = randomClip(rnd);
                setClipboard(clip);
                String before = ed.buffer.getText();
                int selS = ed.isSelectionActive() ? ed.getSelectionStartOffset() : ed.getCaretOffset();
                int selE = ed.isSelectionActive() ? ed.getSelectionEndOffset() : ed.getCaretOffset();
                log.add("ctrl+V clip=" + show(clip) + " selS=" + selS + " selE=" + selE);
                pressKey(ed, KeyEvent.VK_V, CTRL);
                String expected = before.substring(0, selS) + clip + before.substring(selE);
                if (!ed.buffer.getText().equals(expected)) {
                    return fail(seed, log, "PASTE MISMATCH\n--- before:\n" + show(before)
                            + "\n--- expected:\n" + show(expected) + "\n--- actual:\n" + show(ed.buffer.getText()));
                }
            } else if (op < 96) {
                log.add("ctrl+Z");
                pressKey(ed, KeyEvent.VK_Z, CTRL);
            } else if (op < 98) {
                log.add("ctrl+shift+Z");
                pressKey(ed, KeyEvent.VK_Z, CTRL | SHIFT);
            } else if (op < 100) {
                int lc = Math.max(1, lineCount(ed));
                int l1 = rnd.nextInt(lc), l2 = rnd.nextInt(lc);
                int c1 = rnd.nextInt(30), c2 = rnd.nextInt(30);
                if (rnd.nextBoolean()) {
                    log.add("setCaretPosition(" + l1 + "," + c1 + ")");
                    ed.setCaretPosition(l1, c1);
                } else {
                    log.add("setSelection(" + l1 + "," + c1 + "," + l2 + "," + c2 + ")");
                    ed.setSelection(l1, c1, l2, c2);
                }
            } else {
                int line = rnd.nextInt(Math.max(1, lineCount(ed)));
                log.add("toggleFold(" + line + ")");
                ed.toggleFold(line);
            }

            String bad = invariantViolation(ed);
            if (bad != null) {
                return fail(seed, log, "INVARIANT: " + bad + "\n--- text:\n" + show(ed.buffer.getText()));
            }
            int foldedAfter = foldedCount(ed);
            if (!foldOp && foldedAfter > foldedBefore) {
                return fail(seed, log, "FOLD APPEARED: foldedBefore=" + foldedBefore
                        + " foldedAfter=" + foldedAfter + " (edicao escondeu linhas!)"
                        + "\n--- folds: " + describeFolds(ed)
                        + "\n--- text:\n" + show(ed.buffer.getText()));
            }
        }
        return true;
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

    static int lineCount(CodeEditorTextArea ed) {
        return ed.buffer.lineCount();
    }

    static String invariantViolation(CodeEditorTextArea ed) {
        int lc = ed.buffer.lineCount();
        if (ed.caretLine < 0 || ed.caretLine >= lc) {
            return "caretLine=" + ed.caretLine + " lineCount=" + lc;
        }
        int lineLen = ed.buffer.lineAt(ed.caretLine).length();
        if (ed.caretCol < 0 || ed.caretCol > lineLen) {
            return "caretCol=" + ed.caretCol + " > len(" + lineLen + ") on line " + ed.caretLine;
        }
        if (ed.selectionStartLine >= 0) {
            if (ed.selectionStartLine >= lc) {
                return "selectionStartLine=" + ed.selectionStartLine + " lineCount=" + lc;
            }
            int selLen = ed.buffer.lineAt(ed.selectionStartLine).length();
            if (ed.selectionStartCol < 0 || ed.selectionStartCol > selLen) {
                return "selectionStartCol=" + ed.selectionStartCol + " > len(" + selLen
                        + ") on line " + ed.selectionStartLine;
            }
        }
        return null;
    }

    static boolean fail(long seed, List<String> log, String message) {
        System.out.println("==== SEED " + seed + " FAILED ====");
        int from = Math.max(0, log.size() - 20);
        for (int i = from; i < log.size(); i++) {
            System.out.println("  op[" + i + "] " + log.get(i));
        }
        System.out.println(message);
        return false;
    }

    static void pressKey(CodeEditorTextArea ed, int keyCode, int modifiers) {
        KeyEvent e = new KeyEvent(ed, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
        for (KeyListener kl : ed.getKeyListeners()) kl.keyPressed(e);
    }

    static void typeChar(CodeEditorTextArea ed, char c, int modifiers) {
        KeyEvent e = new KeyEvent(ed, KeyEvent.KEY_TYPED, System.currentTimeMillis(),
                modifiers, KeyEvent.VK_UNDEFINED, c);
        for (KeyListener kl : ed.getKeyListeners()) kl.keyTyped(e);
    }

    static void setClipboard(String text) throws Exception {
        for (int attempt = 0; ; attempt++) {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(text), null);
                String read = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getData(DataFlavor.stringFlavor);
                if (text.equals(read)) return;
            } catch (Exception ex) {
                if (attempt >= 5) throw ex;
            }
            Thread.sleep(20);
        }
    }

    static String randomClip(Random rnd) {
        String[] clips = {
                "pasted",
                "line1\nline2",
                "int x = 10;\nint y = 20;\nint z = x + y;",
                "a",
                "\nvoid m() {\n    call();\n}\n",
                "word word word"
        };
        return clips[rnd.nextInt(clips.length)];
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

    static String show(String s) {
        return s.replace("\n", "\\n\n");
    }
}
