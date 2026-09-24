package dtm.stools.component.panels.editor.word.io;

import java.util.LinkedHashMap;
import java.util.Map;

final class DocxNames {
    static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    static final String W_STRICT="http://purl.oclc.org/ooxml/wordprocessingml/main";
    static final String R="http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    static final String R_STRICT="http://purl.oclc.org/ooxml/officeDocument/relationships";
    static final String WP="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing";
    static final String A="http://schemas.openxmlformats.org/drawingml/2006/main";
    static final String PIC="http://schemas.openxmlformats.org/drawingml/2006/picture";
    static final String C="http://schemas.openxmlformats.org/drawingml/2006/chart";
    static final String M="http://schemas.openxmlformats.org/officeDocument/2006/math";
    static final String WPS="http://schemas.microsoft.com/office/word/2010/wordprocessingShape";
    static final String WPG="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup";
    static final String MC="http://schemas.openxmlformats.org/markup-compatibility/2006";
    static final String W14="http://schemas.microsoft.com/office/word/2010/wordml";
    static final String W15="http://schemas.microsoft.com/office/word/2012/wordml";
    static final String WP14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing";
    static final String PKG_RELS="http://schemas.openxmlformats.org/package/2006/relationships";
    static final String CONTENT_TYPES="http://schemas.openxmlformats.org/package/2006/content-types";
    static final String REL_BASE="http://schemas.openxmlformats.org/officeDocument/2006/relationships/";
    static final String REL_IMAGE=REL_BASE+"image",REL_CHART=REL_BASE+"chart",REL_HEADER=REL_BASE+"header",REL_FOOTER=REL_BASE+"footer",REL_HYPERLINK=REL_BASE+"hyperlink",
            REL_STYLES=REL_BASE+"styles",REL_NUMBERING=REL_BASE+"numbering",REL_SETTINGS=REL_BASE+"settings",REL_FOOTNOTES=REL_BASE+"footnotes",REL_ENDNOTES=REL_BASE+"endnotes",
            REL_COMMENTS=REL_BASE+"comments",REL_PACKAGE=REL_BASE+"package",REL_CUSTOM_XML=REL_BASE+"customXml",REL_CUSTOM_XML_PROPS=REL_BASE+"customXmlProps",
            REL_COMMENTS_EXTENDED="http://schemas.microsoft.com/office/2011/relationships/commentsExtended";
    static final String CT_MAIN="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";
    static final String CT_STYLES="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml";
    static final String CT_NUMBERING="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml";
    static final String CT_SETTINGS="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml";
    static final String CT_HEADER="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml";
    static final String CT_FOOTER="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml";
    static final String CT_FOOTNOTES="application/vnd.openxmlformats-officedocument.wordprocessingml.footnotes+xml";
    static final String CT_ENDNOTES="application/vnd.openxmlformats-officedocument.wordprocessingml.endnotes+xml";
    static final String CT_COMMENTS="application/vnd.openxmlformats-officedocument.wordprocessingml.comments+xml";
    static final String CT_COMMENTS_EXTENDED="application/vnd.openxmlformats-officedocument.wordprocessingml.commentsExtended+xml";
    static final String CT_CHART="application/vnd.openxmlformats-officedocument.drawingml.chart+xml";
    static final String CT_XLSX="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final String CT_CUSTOM_PROPS="application/vnd.openxmlformats-officedocument.customXmlProperties+xml";
    static final Map<String,String> ROOT_NAMESPACES=new LinkedHashMap<>();
    static {
        ROOT_NAMESPACES.put("wpc","http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas");
        ROOT_NAMESPACES.put("mc",MC);
        ROOT_NAMESPACES.put("o","urn:schemas-microsoft-com:office:office");
        ROOT_NAMESPACES.put("r",R);
        ROOT_NAMESPACES.put("m",M);
        ROOT_NAMESPACES.put("v","urn:schemas-microsoft-com:vml");
        ROOT_NAMESPACES.put("wp14",WP14);
        ROOT_NAMESPACES.put("wp",WP);
        ROOT_NAMESPACES.put("w10","urn:schemas-microsoft-com:office:word");
        ROOT_NAMESPACES.put("w14",W14);
        ROOT_NAMESPACES.put("w15",W15);
        ROOT_NAMESPACES.put("wpg",WPG);
        ROOT_NAMESPACES.put("wps",WPS);
        ROOT_NAMESPACES.put("a",A);
        ROOT_NAMESPACES.put("pic",PIC);
        ROOT_NAMESPACES.put("c",C);
    }
    private DocxNames() {}
    static boolean isWord(String ns) { return W.equals(ns) || W_STRICT.equals(ns); }
    static String relType(String type) { int i=type.lastIndexOf('/'); return i<0?type:type.substring(i+1); }
}
