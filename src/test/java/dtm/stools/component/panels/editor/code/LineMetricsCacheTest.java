package dtm.stools.component.panels.editor.code;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineMetricsCacheTest {

    @Test
    void replaceRangeKeepsValuesOutsideTheEditAndMarksEditedLinesUnknown() {
        LineMetricsCache cache = filled(10, 11, 12, 13, 14);

        cache.replaceRange(1, 0, 2);
        assertTrue(cache.isValidFor(7));
        assertValues(cache, 10, -1, -1, -1, 12, 13, 14);

        cache.replaceRange(2, 3, 0);
        assertTrue(cache.isValidFor(4));
        assertValues(cache, 10, -1, -1, 14);

        cache.replaceRange(3, 0, 0);
        assertValues(cache, 10, -1, -1, -1);
    }

    @Test
    void growsBeyondInitialCapacity() {
        LineMetricsCache cache = filled(1, 2);
        cache.replaceRange(1, 0, 100);
        assertTrue(cache.isValidFor(102));
        assertEquals(1, cache.get(0));
        for (int i = 1; i < 102; i++) {
            assertEquals(LineMetricsCache.UNKNOWN, cache.get(i));
        }
    }

    @Test
    void inconsistentRangeInvalidatesInsteadOfCorrupting() {
        LineMetricsCache cache = filled(1, 2, 3);
        cache.replaceRange(2, 5, 0);
        assertFalse(cache.isValidFor(3));
        assertFalse(cache.isValidFor(-2));
        cache.replaceRange(0, 0, 1);
        assertFalse(cache.isValidFor(4));
    }

    private static LineMetricsCache filled(int... values) {
        LineMetricsCache cache = new LineMetricsCache();
        cache.reset(values.length);
        for (int i = 0; i < values.length; i++) {
            cache.set(i, values[i]);
        }
        return cache;
    }

    private static void assertValues(LineMetricsCache cache, int... expected) {
        assertTrue(cache.isValidFor(expected.length));
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], cache.get(i), "line " + i);
        }
    }
}
