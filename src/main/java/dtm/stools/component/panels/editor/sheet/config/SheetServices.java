package dtm.stools.component.panels.editor.sheet.config;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.io.CsvCodec;
import dtm.stools.component.panels.editor.sheet.io.OdsCodec;
import dtm.stools.component.panels.editor.sheet.io.XlsxCodec;
import dtm.stools.component.panels.editor.sheet.render.SheetRenderer;
import dtm.stools.component.panels.editor.sheet.ui.SheetUiFactory;

import java.util.Objects;

public record SheetServices(XlsxCodec xlsx, CsvCodec csv, OdsCodec ods, FunctionRegistry functions, SheetRenderer renderer, SheetUiFactory uiFactory) {
    private static volatile FunctionRegistry sharedFunctions;

    public SheetServices {
        Objects.requireNonNull(xlsx); Objects.requireNonNull(csv); Objects.requireNonNull(ods);
        Objects.requireNonNull(functions); Objects.requireNonNull(renderer); Objects.requireNonNull(uiFactory);
    }

    public static SheetServices defaults() {
        return new SheetServices(new XlsxCodec(), new CsvCodec(), new OdsCodec(), defaultFunctions().copy(), new SheetRenderer(), SheetUiFactory.defaults());
    }

    public static FunctionRegistry defaultFunctions() {
        FunctionRegistry f = sharedFunctions;
        if (f == null) {
            synchronized (SheetServices.class) {
                if (sharedFunctions == null) sharedFunctions = FunctionRegistry.defaults();
                f = sharedFunctions;
            }
        }
        return f;
    }

    public CalcEngine calc() { return new CalcEngine(functions); }

    public SheetServices withFunctions(FunctionRegistry value) { return new SheetServices(xlsx, csv, ods, value, renderer, uiFactory); }
    public SheetServices withRenderer(SheetRenderer value) { return new SheetServices(xlsx, csv, ods, functions, value, uiFactory); }
    public SheetServices withUiFactory(SheetUiFactory value) { return new SheetServices(xlsx, csv, ods, functions, renderer, value); }
}
