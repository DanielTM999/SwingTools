package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;

public final class Libraries {
    private Libraries() {}

    public static void registerAll(FunctionRegistry r) {
        MathFunctions.register(r);
        LogicalFunctions.register(r);
        InformationFunctions.register(r);
        TextFunctions.register(r);
        DateTimeFunctions.register(r);
        LookupFunctions.register(r);
        StatisticalFunctions.register(r);
        ArrayFunctions.register(r);
        FinancialFunctions.register(r);
        EngineeringFunctions.register(r);
        DatabaseFunctions.register(r);
        WebCubeFunctions.register(r);
        CompatibilityFunctions.register(r);
        GoogleFunctions.register(r);
    }
}
