package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.inlay.InlayHint;
import dtm.stools.component.panels.editor.code.inlay.InlayHintKind;
import dtm.stools.component.panels.editor.code.prototype.styles.StyledRange;
import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;
import org.junit.jupiter.api.Test;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CodeEditorColumnHitTest {

    private static final String LONG = "    the quick brown fox jumps over the lazy dog and keeps running far beyond the visible area of the editor";

    @Test
    void plainTextMatchesLegacyScan() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area("public static void main(String[] args) { return; }\nshort", 2000);
            assertMatchesLegacy(area);
        });
    }

    @Test
    void tabsMatchLegacyScan() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area("\tif (a)\t{ b =\t\tc; }\n  \t mixed\tindent\t", 2000);
            assertMatchesLegacy(area);
        });
    }

    @Test
    void styledRunsMatchLegacyScan() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String text = "int value = compute(alpha, beta) + other.call(\"text\", 42);\n\tbold\titalic\tplain";
            Area area = new Area(text, 2000);
            TextStyle bold = TextStyle.builder().bold(true).build();
            TextStyle italic = TextStyle.builder().italic(true).build();
            TextStyle both = TextStyle.builder().bold(true).italic(true).build();
            area.addStyledRange(new StyledRange(bold, 0, 3));
            area.addStyledRange(new StyledRange(italic, 4, 9));
            area.addStyledRange(new StyledRange(both, 12, 19));
            area.addStyledRange(new StyledRange(bold, 20, 25));
            area.addStyledRange(new StyledRange(italic, 46, 52));
            int second = area.offsetOf(1, 0);
            area.addStyledRange(new StyledRange(bold, second + 1, second + 5));
            area.addStyledRange(new StyledRange(italic, second + 6, second + 12));
            assertMatchesLegacy(area);
        });
    }

    @Test
    void wrappedRowsMatchLegacyScan() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area("short\n" + LONG + "\n\t" + LONG + "\nend", 240);
            area.setLineWrapEnabled(true);
            area.addStyledRange(new StyledRange(TextStyle.builder().bold(true).build(),
                    area.offsetOf(1, 10), area.offsetOf(1, 30)));
            assertTrue(area.wrapRowCount(1) > 2);
            assertTrue(area.wrapRowCount(2) > 2);
            assertMatchesLegacy(area);
        });
    }

    @Test
    void ghostTextMatchesLegacyScan() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area("String name = value.toString();\nnext line", 2000);
            area.setCaretPosition(0, 14);
            area.showGhost("suggested()", 0, 14);
            assertTrue(area.hasGhostText());
            assertTrue(area.ghostPushWidthBeforeColumn(0, 20, area.metrics()) > 0);
            assertMatchesLegacy(area);
        });
    }

    @Test
    void pushedInlayHintsMatchLegacyScan() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area("call(first, second, third);\n\tcall(a,\tb);", 2000);
            area.inlayHints.add(new InlayHint(0, 5, "a:", InlayHintKind.OTHER, true, true, null, null, true));
            area.inlayHints.add(new InlayHint(0, 12, "b:", InlayHintKind.OTHER, true, true, null, null, true));
            area.inlayHints.add(new InlayHint(0, 20, ": int", InlayHintKind.TYPE));
            area.inlayHints.add(new InlayHint(1, 6, "x:", InlayHintKind.OTHER, true, true, null, null, true));
            area.invalidateGeometry();
            assertMatchesLegacy(area);
        });
    }

    @Test
    void emptyAndSingleCharLinesMatchLegacyScan() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area("a\n\nxyz\n\t\n ", 2000);
            assertMatchesLegacy(area);
        });
    }

    @Test
    void positionFromPointKeepsColumnContract() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String text = "abc\tdef";
            Area area = new Area(text, 2000);
            int y = area.yOfBufferLine(0) + 2;

            assertArrayEquals(new int[]{0, 0}, area.positionFromPoint(0, y));
            assertArrayEquals(new int[]{0, text.length()}, area.positionFromPoint(10_000, y));
            for (int col = 0; col < text.length(); col++) {
                int left = area.visualXForColumn(0, col);
                int right = area.visualXForColumn(0, col + 1);
                assertArrayEquals(new int[]{0, col}, area.positionFromPoint(left + 1, y), "left of " + col);
                assertArrayEquals(new int[]{0, col + 1}, area.positionFromPoint(right - 1, y), "right of " + col);
            }
        });
    }

    @Test
    void arrowUpDownKeepsColumnOnWrappedLine() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area("short\n" + LONG + "\nend", 240);
            area.setLineWrapEnabled(true);
            area.setCaretPosition(1, 6);
            int start = area.wrapRowStartColumn(1, 1);

            area.press(KeyEvent.VK_DOWN);
            assertEquals(1, area.getCaretLine());
            assertTrue(area.getCaretCol() >= start);
            assertEquals(1, area.wrapRowOfColumn(1, area.getCaretCol()));

            area.press(KeyEvent.VK_UP);
            assertEquals(1, area.getCaretLine());
            assertEquals(6, area.getCaretCol());
        });
    }

    @Test
    void hitTestingOnHugeSingleLineIsFast() throws Exception {
        String text = sourceMapLine(200_000);
        AtomicReference<Area> holder = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> holder.set(new Area(text, 1200)));

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> SwingUtilities.invokeAndWait(() -> {
            Area area = holder.get();
            int y = area.yOfBufferLine(0) + 2;
            int length = area.lineText(0).length();
            for (int i = 0; i < 50; i++) {
                int col = (int) ((long) length * i / 50);
                int x = area.visualXForColumn(0, col);
                assertArrayEquals(new int[]{0, col}, area.positionFromPoint(x, y), "column " + col);
            }
            int endX = area.visualXForColumn(0, length);
            assertArrayEquals(new int[]{0, length}, area.positionFromPoint(endX + 100, y));
            assertArrayEquals(new int[]{0, 0}, area.positionFromPoint(0, y));
        }));
    }

    @Test
    void arrowKeysOnHugeWrappedLineAreFast() throws Exception {
        String text = sourceMapLine(200_000) + "\nlast";
        AtomicReference<Area> holder = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(text, 600);
            area.setLineWrapEnabled(true);
            area.setCaretPosition(0, 3);
            holder.set(area);
        });

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> SwingUtilities.invokeAndWait(() -> {
            Area area = holder.get();
            for (int i = 0; i < 20; i++) {
                area.press(KeyEvent.VK_DOWN);
            }
            assertEquals(0, area.getCaretLine());
            assertEquals(20, area.wrapRowOfColumn(0, area.getCaretCol()));
            for (int i = 0; i < 20; i++) {
                area.press(KeyEvent.VK_UP);
            }
            assertEquals(0, area.getCaretLine());
            assertEquals(3, area.getCaretCol());
        }));
    }

    private static String sourceMapLine(int length) {
        StringBuilder builder = new StringBuilder(length + 64);
        builder.append("{\"version\":3,\"sources\":[\"windowViewer.ts\"],\"names\":[],\"mappings\":\"");
        String[] segments = {"AAAA", "CAAC", "EAAE", "GAAG", "IAAI", "KAAK"};
        int index = 0;
        while (builder.length() < length) {
            builder.append(segments[index % segments.length]);
            builder.append(index % 7 == 6 ? ';' : ',');
            index++;
        }
        builder.append("\"}");
        return builder.toString();
    }

    private static void assertMatchesLegacy(Area area) {
        FontMetrics fm = area.metrics();
        for (int line = 0; line < area.lineCount(); line++) {
            String lineText = area.lineText(line);
            for (int flags = 0; flags < 4; flags++) {
                boolean inlayPush = (flags & 1) != 0;
                boolean ghostPush = (flags & 2) != 0;
                assertEquals(area.legacyRuns(line, inlayPush, ghostPush), area.currentRuns(line, inlayPush, ghostPush),
                        "runs of line " + line + " inlay " + inlayPush + " ghost " + ghostPush);
            }
            int maxX = Math.max(area.viewport.getWidth(), area.visualXForColumn(line, lineText.length())) + 60;
            int rows = area.wrapRowCount(line);
            for (int row = 0; row < rows; row++) {
                for (int mx = -20; mx <= maxX; mx++) {
                    int expected = area.legacyColumnAtRowX(line, lineText, row, mx, fm);
                    int actual = area.columnAtRowX(line, lineText, row, mx, fm);
                    assertEquals(expected, actual, "line " + line + " row " + row + " x " + mx);
                }
            }
        }
    }

    private static class Area extends CodeEditorTextArea {
        final JViewport viewport = new JViewport();

        Area(String text, int width) {
            super(text);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            viewport.setView(this);
            viewport.setSize(new Dimension(width, 300));
        }

        int lineCount() { return buffer.lineCount(); }
        String lineText(int line) { return buffer.lineAt(line); }
        int offsetOf(int line, int col) { return buffer.offsetOfLine(line) + col; }
        FontMetrics metrics() { return fontMetricsFor(getFont()); }

        void showGhost(String text, int line, int col) {
            setActiveGhostText(text, line, col, offsetOf(line, col));
        }

        void press(int keyCode) {
            KeyEvent event = new KeyEvent(this, KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
            for (KeyListener listener : getKeyListeners()) {
                listener.keyPressed(event);
            }
        }

        List<String> currentRuns(int line, boolean inlayPush, boolean ghostPush) {
            List<String> runs = new ArrayList<>();
            String lineText = buffer.lineAt(line);
            int endX = forEachLineRun(line, lineText, lineText.length(), getFont(), metrics(), inlayPush, ghostPush,
                    (startCol, endCol, visualCol, run, style, font, runFm, x, runWidth, row) -> {
                        runs.add(describeRun(startCol, endCol, visualCol, run, style, font, x, runWidth, row));
                        return true;
                    });
            runs.add("end " + endX);
            return runs;
        }

        List<String> legacyRuns(int line, boolean inlayPush, boolean ghostPush) {
            List<String> runs = new ArrayList<>();
            String lineText = buffer.lineAt(line);
            int endX = legacyForEachLineRun(line, lineText, lineText.length(), getFont(), metrics(), inlayPush, ghostPush,
                    wrapBreaks(line),
                    (startCol, endCol, visualCol, run, style, font, runFm, x, runWidth, row) -> {
                        runs.add(describeRun(startCol, endCol, visualCol, run, style, font, x, runWidth, row));
                        return true;
                    });
            runs.add("end " + endX);
            return runs;
        }

        private static String describeRun(int startCol, int endCol, int visualCol, String run, TextStyle style,
                                          Font font, int x, int runWidth, int row) {
            return startCol + ":" + endCol + ":" + visualCol + ":" + run + ":" + System.identityHashCode(style)
                    + ":" + font.getStyle() + ":" + x + ":" + runWidth + ":" + row;
        }

        int legacyForEachLineRun(int line,
                                 String lineText,
                                 int renderLength,
                                 Font baseFont,
                                 FontMetrics defaultFm,
                                 boolean includeInlayPush,
                                 boolean includeGhostPush,
                                 int[] breaks,
                                 LineRunVisitor visitor) {
            int lineOffset = buffer.offsetOfLine(line);
            int x = TEXT_LEFT_MARGIN;
            int col = 0;
            int visualCol = 0;
            int breakIndex = 0;
            int row = 0;
            int wrapIndent = breaks.length > 0 ? wrapIndentX(lineText, defaultFm) : 0;

            List<InlayHint> pushHints = pushInlayHintsForLine(line, defaultFm);
            int pushHintIndex = 0;

            int ghostPushCol = (line == ghostAnchorLine && isGhostVisibleAtAnchor()) ? ghostAnchorCol : -1;
            boolean ghostPushApplied = false;

            while (col < renderLength) {
                while (breakIndex < breaks.length && breaks[breakIndex] <= col) {
                    breakIndex++;
                    row++;
                    x = TEXT_LEFT_MARGIN + wrapIndent;
                }
                while (pushHintIndex < pushHints.size()
                        && inlayHintColumn(pushHints.get(pushHintIndex), lineText) <= col) {
                    if (includeInlayPush) x += inlayHintWidth(defaultFm, pushHints.get(pushHintIndex));
                    pushHintIndex++;
                }
                if (ghostPushCol >= 0 && !ghostPushApplied && ghostPushCol <= col) {
                    if (includeGhostPush) x += ghostFirstSegmentWidth(defaultFm);
                    ghostPushApplied = true;
                }

                TextStyle style = getStyleAt(lineOffset + col);
                int runEnd = col + 1;
                while (runEnd < renderLength && getStyleAt(lineOffset + runEnd) == style) {
                    runEnd++;
                }
                if (pushHintIndex < pushHints.size()) {
                    int pushCol = inlayHintColumn(pushHints.get(pushHintIndex), lineText);
                    if (pushCol > col && pushCol < runEnd) {
                        runEnd = pushCol;
                    }
                }
                if (ghostPushCol > col && ghostPushCol < runEnd) {
                    runEnd = ghostPushCol;
                }
                if (breakIndex < breaks.length && breaks[breakIndex] < runEnd) {
                    runEnd = breaks[breakIndex];
                }

                String run = expandTabs(lineText.substring(col, runEnd), visualCol);
                Font font = deriveFont(baseFont, style);
                FontMetrics fm = fontMetricsFor(font);
                int runWidth = fm.stringWidth(run);

                if (!visitor.visit(col, runEnd, visualCol, run, style, font, fm, x, runWidth, row)) {
                    return x;
                }

                x += runWidth;
                visualCol += run.length();
                col = runEnd;
            }

            while (pushHintIndex < pushHints.size()
                    && inlayHintColumn(pushHints.get(pushHintIndex), lineText) <= renderLength) {
                if (includeInlayPush) x += inlayHintWidth(defaultFm, pushHints.get(pushHintIndex));
                pushHintIndex++;
            }
            if (includeGhostPush && ghostPushCol >= 0 && !ghostPushApplied && ghostPushCol <= renderLength) {
                x += ghostFirstSegmentWidth(defaultFm);
            }
            return x;
        }

        int legacyColumnAtRowX(int line, String lineText, int row, int mx, FontMetrics fm) {
            int[] breaks = wrapBreaks(line);
            int safeRow = Math.max(0, Math.min(row, breaks.length));
            int rowStart = safeRow == 0 ? 0 : breaks[safeRow - 1];
            boolean lastRow = safeRow == breaks.length;
            int rowEnd = lastRow ? lineText.length() : breaks[safeRow] - 1;
            int[] best = {rowStart, Math.abs(mx - (visualXForColumn(line, lineText, rowStart, fm)))};
            int endX = forEachLineRun(
                    line,
                    lineText,
                    lineText.length(),
                    getFont(),
                    fm,
                    true,
                    false,
                    (startCol, endCol, visualCol, run, style, font, runFm, x, runWidth, runRow) -> {
                        if (runRow < safeRow) return true;
                        if (runRow > safeRow) return false;
                        for (int c = Math.max(startCol, rowStart); c <= Math.min(endCol, rowEnd); c++) {
                            int cx = (c == startCol)
                                    ? x
                                    : x + runFm.stringWidth(expandTabs(lineText.substring(startCol, c), visualCol));
                            int dist = Math.abs(mx - (cx + ghostPushWidthBeforeColumn(line, c, fm)));
                            if (dist < best[1]) {
                                best[1] = dist;
                                best[0] = c;
                            }
                        }
                        return true;
                    });
            if (lastRow) {
                int endDist = Math.abs(mx - (endX + ghostPushWidthBeforeColumn(line, lineText.length(), fm)));
                if (endDist < best[1]) {
                    best[0] = lineText.length();
                }
            }
            return best[0];
        }
    }
}
