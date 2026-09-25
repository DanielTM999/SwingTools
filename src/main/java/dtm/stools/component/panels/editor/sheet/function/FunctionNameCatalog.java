package dtm.stools.component.panels.editor.sheet.function;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class FunctionNameCatalog {
    private static volatile FunctionNameCatalog portuguese;

    private final Map<String, String> localized = new HashMap<>();
    private final Map<String, String> canonical = new HashMap<>();

    public FunctionNameCatalog(Map<String, String> canonicalToLocalized) {
        canonicalToLocalized.forEach(this::put);
    }

    public static FunctionNameCatalog portuguese() {
        FunctionNameCatalog c = portuguese;
        if (c == null) {
            synchronized (FunctionNameCatalog.class) {
                if (portuguese == null) portuguese = load("functions_pt_BR.properties");
                c = portuguese;
            }
        }
        return c;
    }

    public static FunctionNameCatalog load(String resource) {
        Properties p = new Properties();
        try (InputStream in = FunctionNameCatalog.class.getResourceAsStream("/dtm/stools/component/panels/editor/sheet/" + resource)) {
            if (in != null) p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> map = new HashMap<>();
        for (String k : p.stringPropertyNames()) map.put(k, p.getProperty(k));
        return new FunctionNameCatalog(map);
    }

    public synchronized void put(String canonicalName, String localizedName) {
        String c = canonicalName.toUpperCase(Locale.ROOT), l = localizedName.toUpperCase(Locale.ROOT);
        localized.put(c, l);
        canonical.put(l, c);
    }

    public synchronized String localize(String canonicalName) {
        String c = canonicalName.toUpperCase(Locale.ROOT);
        return localized.getOrDefault(c, c);
    }

    public synchronized String canonical(String name) {
        String u = name.toUpperCase(Locale.ROOT);
        if (u.startsWith("_XLFN.")) u = u.substring(6);
        if (u.startsWith("_XLWS.")) u = u.substring(6);
        String c = canonical.get(u);
        return c != null ? c : u;
    }

    public synchronized Map<String, String> entries() { return Map.copyOf(localized); }
}
