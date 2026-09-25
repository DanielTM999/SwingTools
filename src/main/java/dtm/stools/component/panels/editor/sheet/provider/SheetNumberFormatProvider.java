package dtm.stools.component.panels.editor.sheet.provider;

import java.util.List;
import java.util.Map;

public interface SheetNumberFormatProvider extends SheetProvider {
    Map<String, List<String>> formats();
}
