package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.function.SheetFunction;

import java.util.List;
import java.util.Map;

public interface SheetFunctionProvider extends SheetProvider {
    List<SheetFunction> functions();

    default Map<String, String> localizedNames() { return Map.of(); }
}
