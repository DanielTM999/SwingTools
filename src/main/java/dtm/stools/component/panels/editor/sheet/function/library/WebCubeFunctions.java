package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionCategory;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionDefinition;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.provider.ExternalDataRequest;
import dtm.stools.component.panels.editor.sheet.provider.SheetExternalDataProvider;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.CUBE;
import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.WEB;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class WebCubeFunctions {
    private WebCubeFunctions() {}

    static void register(FunctionRegistry r) {
        scalar(r, "ENCODEURL", WEB, 1, 1, (c, a) -> text(URLEncoder.encode(t(a, 0), StandardCharsets.UTF_8).replace("+", "%20")));
        raw(r, "FILTERXML", WEB, 2, 2, (c, a) -> {
            try {
                DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
                f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                f.setExpandEntityReferences(false);
                Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(a.text(0).getBytes(StandardCharsets.UTF_8)));
                NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath().evaluate(a.text(1), doc, XPathConstants.NODESET);
                if (nodes.getLength() == 0) throw EvalError.value();
                List<CellValue> out = new ArrayList<>();
                for (int k = 0; k < nodes.getLength(); k++) {
                    String s = nodes.item(k).getTextContent();
                    Double d = dtm.stools.component.panels.editor.sheet.format.ValueParser.parseNumberLenient(s);
                    out.add(d != null ? num(d) : text(s));
                }
                return out.size() == 1 ? out.getFirst() : ArrayValue.column(out);
            } catch (EvalError e) { throw e; } catch (Exception e) { throw EvalError.value(); }
        });
        external(r, "WEBSERVICE", WEB, 1, 1, false);
        external(r, "STOCKHISTORY", FunctionCategory.FINANCIAL, 2, 11, true);
        for (String name : new String[]{"CUBEKPIMEMBER", "CUBEMEMBER", "CUBEMEMBERPROPERTY", "CUBERANKEDMEMBER", "CUBESET", "CUBESETCOUNT", "CUBEVALUE"}) external(r, name, CUBE, 1, 255, false);
        r.register(FunctionDefinition.raw("IMAGE", FunctionCategory.LOOKUP, 1, 5, (c, a) -> text(a.text(0))).modern().build());
    }

    static void external(FunctionRegistry r, String name, FunctionCategory category, int min, int max, boolean modern) {
        FunctionDefinition.Builder b = FunctionDefinition.raw(name, category, min, max, (c, a) -> fetch(c, a, name)).volatileFunction();
        if (modern) b.modern();
        r.register(b.build());
    }

    static CellValue fetch(FunctionContext c, FunctionArgs a, String name) {
        SheetExternalDataProvider p = c.external();
        if (p == null || !p.supports(name)) return err(CellError.CONNECT);
        List<String> args = new ArrayList<>();
        for (int k = 0; k < a.size(); k++) args.add(a.has(k) ? Coerce.text(a.scalar(k)) : "");
        Optional<CellValue> v = p.fetch(new ExternalDataRequest(name, args, c.sheetName(c.hostSheet()), c.host().toA1()));
        return v.orElse(err(CellError.GETTING_DATA));
    }
}
