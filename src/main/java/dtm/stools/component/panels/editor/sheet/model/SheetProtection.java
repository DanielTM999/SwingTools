package dtm.stools.component.panels.editor.sheet.model;

import lombok.Builder;
import lombok.With;

@With
@Builder(toBuilder = true)
public record SheetProtection(boolean enabled, String passwordHash, boolean selectLocked, boolean selectUnlocked, boolean formatCells, boolean formatColumns,
                              boolean formatRows, boolean insertColumns, boolean insertRows, boolean insertHyperlinks, boolean deleteColumns, boolean deleteRows,
                              boolean sort, boolean autoFilter, boolean pivotTables, boolean objects, boolean scenarios) {
    public static final SheetProtection NONE = new SheetProtection(false, null, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false);

    public static SheetProtection protect(String passwordHash) { return NONE.withEnabled(true).withPasswordHash(passwordHash); }

    public static String hash(String password) {
        if (password == null || password.isEmpty()) return null;
        int h = 0;
        for (int i = password.length() - 1; i >= 0; i--) { h = ((h >> 14) & 1) | ((h << 1) & 0x7fff); h ^= password.charAt(i); }
        h = ((h >> 14) & 1) | ((h << 1) & 0x7fff);
        h ^= password.length(); h ^= 0xCE4B;
        return String.format("%04X", h);
    }

    public boolean verify(String password) { return passwordHash == null || passwordHash.equalsIgnoreCase(String.valueOf(hash(password))); }
}
