package dtm.stools.component.panels.editor.word;

import dtm.stools.component.panels.editor.word.api.WordSession;
import dtm.stools.component.panels.editor.word.controller.WordDocumentController;
import dtm.stools.component.panels.editor.word.io.*;
import dtm.stools.component.panels.editor.word.io.ooxml.*;
import dtm.stools.component.panels.editor.word.layout.*;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.ui.popup.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static dtm.stools.component.panels.editor.word.model.WordParagraphStyle.LineSpacingRule.*;
import static dtm.stools.component.panels.editor.word.model.WordSectionProperties.BreakType.*;

class WordFidelityTest {
    private final DocxCodec codec=new DocxCodec();
    private static final WordPageSettings SMALL=new WordPageSettings(250,180,20,20,20,20);
    private static WordParagraph paragraph(String text,float height){
        return WordParagraph.of(text,WordTextStyle.DEFAULT.withFamily("Dialog").withSize(10),
                WordParagraphStyle.DEFAULT.withSpacing(0,0,1).withLineSpacing(EXACT,height).withWidowControl(false));
    }
    private byte[] write(WordDocument document,WordImportResult origin)throws Exception {
        var out=new ByteArrayOutputStream();codec.write(document,origin,out);return out.toByteArray();
    }
    private WordImportResult read(byte[] bytes)throws Exception{return codec.read(new ByteArrayInputStream(bytes));}
    private byte[] patch(byte[] bytes,String part,String from,String to)throws Exception {
        var source=OpcPackage.read(bytes,OpcPackage.Limits.DEFAULT);var out=new ByteArrayOutputStream();
        source.withPart(part,new String(source.part(part),StandardCharsets.UTF_8).replace(from,to).getBytes(StandardCharsets.UTF_8)).write(out);
        return out.toByteArray();
    }
    private String xml(byte[] bytes,String part)throws Exception{return new String(OpcPackage.read(bytes,OpcPackage.Limits.DEFAULT).part(part),StandardCharsets.UTF_8);}
    private static WordPageSettings section(WordPageSettings settings,WordSectionProperties.BreakType type,String header){
        var props=settings.section().withBreakType(type);
        if(header!=null)props=props.withHeaders(WordHeaders.EMPTY.with(WordHeaders.Kind.HEADER,List.of(paragraph(header,12))))
                .withLinked(WordHeaders.Kind.HEADER,false);
        return settings.withSection(props);
    }
    private static List<WordLayout.Line> body(WordLayout.Page page){return page.lines().stream().filter(WordLayout.Line::positional).toList();}
    private static WordParagraph lines(int count,boolean widow){
        List<WordInline> runs=new ArrayList<>();
        for(int i=0;i<count;i++){
            if(i>0)runs.add(new WordObjectRun(WordBreak.of(WordBreak.Kind.LINE),WordTextStyle.DEFAULT.withSize(10)));
            runs.add(new WordRun("row "+i,WordTextStyle.DEFAULT.withSize(10)));
        }
        return paragraph("",20).withRuns(runs).withStyle(paragraph("",20).style().withWidowControl(widow));
    }
    private static int pageOf(WordLayout layout,int offset){
        return layout.pages().stream().filter(p->body(p).stream().anyMatch(l->l.start()<=offset&&l.end()>=offset)).findFirst().orElseThrow().index();
    }

    @Test void exactAndMinimumSpacingSurviveEditingAndReopening()throws Exception {
        for(var rule:List.of(EXACT,AT_LEAST)){
            var style=WordParagraphStyle.DEFAULT.withLineSpacing(rule,18.5f).withKeepLines(true).withKeepWithNext(true).withWidowControl(false);
            var doc=new WordDocument(List.of(WordParagraph.of("mixed",WordTextStyle.DEFAULT,style)),SMALL);
            var imported=read(write(doc,null));assertEquals(style,imported.document().paragraphs().getFirst().style());
            var edited=imported.document().replace(0,0,"edited ",WordTextStyle.DEFAULT);
            byte[] saved=write(edited,imported);assertTrue(xml(saved,"word/document.xml").contains("w:line=\"370\""));
            assertEquals(style,read(saved).document().paragraphs().getFirst().style());
            assertTrue(imported.diagnostics().stream().noneMatch(d->d.contains("Espaçamento")));
        }
    }
    @Test void spacingAndPaginationInheritFromStylesAndAllowExplicitFalse()throws Exception {
        var props=WordStyleProperties.NONE.withParagraph(null,null,null,24f,null,true).withPagination(EXACT,true,true).withPageBreakBefore(true);
        var styles=WordStyleSheet.defaults().with(new WordNamedStyle("Custom","Custom",WordStyleSheet.NORMAL,props))
                .with(new WordNamedStyle("Child","Child","Custom",WordStyleProperties.NONE));
        var doc=new WordDocument(List.of(WordParagraph.of("styled",WordTextStyle.DEFAULT,styles.resolveParagraph("Child"))),SMALL,WordParts.EMPTY.withStyles(styles));
        byte[] bytes=write(doc,null);
        var packageFile=OpcPackage.read(bytes,OpcPackage.Limits.DEFAULT);
        var dom=OoxmlXml.parse(packageFile.part("word/document.xml"));
        var pr=OoxmlXml.path(dom.getDocumentElement(),"body","p","pPr");
        for(var element:new ArrayList<>(OoxmlXml.children(pr)))if(Set.of("spacing","keepLines","keepNext","widowControl","pageBreakBefore").contains(element.getLocalName()))pr.removeChild(element);
        var out=new ByteArrayOutputStream();packageFile.withPart("word/document.xml",OoxmlXml.bytes(dom)).write(out);
        var imported=read(out.toByteArray());var inherited=imported.document().paragraphs().getFirst().style();
        assertEquals(EXACT,inherited.lineSpacingRule());assertEquals(24,inherited.lineSpacing());assertTrue(inherited.keepLines());assertTrue(inherited.keepWithNext());assertTrue(inherited.pageBreakBefore());
        var edited=imported.document().formatParagraphs(0,0,s->s.withKeepLines(false).withKeepWithNext(false).withWidowControl(false).withPageBreakBefore(false));
        var reopened=read(write(edited,imported)).document().paragraphs().getFirst().style();
        assertFalse(reopened.keepLines());assertFalse(reopened.keepWithNext());assertFalse(reopened.widowControl());assertFalse(reopened.pageBreakBefore());
    }
    @Test void exactLineBoxesUsePointsAndClipOversizedText(){
        var p=paragraph("Large text wraps into several lines",10)
                .withRuns(List.of(new WordRun("Large text wraps into several lines",WordTextStyle.DEFAULT.withSize(30))));
        var lines=body(new WordLayoutEngine().layout(new WordDocument(List.of(p),SMALL)).pages().getFirst());
        assertTrue(lines.size()>1);assertEquals(10,lines.get(1).baseline()-lines.get(0).baseline(),0.01);
        assertTrue(lines.getFirst().clipToBox());assertEquals(10,lines.getFirst().bottom()-lines.getFirst().top(),0.01);
    }
    @Test void minimumSpacingExpandsForLargeRuns(){
        var p=paragraph("large",10).withStyle(paragraph("",10).style().withLineSpacing(AT_LEAST,10))
                .withRuns(List.of(new WordRun("large",WordTextStyle.DEFAULT.withSize(35))));
        var line=body(new WordLayoutEngine().layout(new WordDocument(List.of(p),SMALL)).pages().getFirst()).getFirst();
        assertTrue(line.boxHeight()>35);assertFalse(line.clipToBox());
    }
    @Test void keepsParagraphTogetherAndFallsBackForOversizedParagraph(){
        var first=paragraph("filler",100);var together=paragraph("one two three four five six seven eight nine ten ".repeat(3),20).withStyle(paragraph("",20).style().withKeepLines(true));
        var doc=new WordDocument(List.of(first,together),SMALL);var layout=new WordLayoutEngine().layout(doc);
        assertEquals(1,pageOf(layout,doc.paragraphStart(1)));
        var huge=together.withRuns(List.of(new WordRun("long paragraph ".repeat(100),WordTextStyle.DEFAULT.withSize(10))));
        var hugeDoc=new WordDocument(List.of(huge),SMALL);assertTrue(new WordLayoutEngine().layout(hugeDoc).pages().size()>1);
    }
    @Test void avoidsOrphansAndWidowsAtPageBoundaries(){
        var filler=paragraph("filler",100);var text=paragraph("one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen twenty",20);
        var style=text.style().withWidowControl(true);text=text.withStyle(style);
        var doc=new WordDocument(List.of(filler,text),SMALL);var layout=new WordLayoutEngine().layout(doc);
        int offset=doc.paragraphStart(1);
        for(var page:layout.pages()){
            long count=body(page).stream().filter(l->l.start()>=offset).count();
            assertTrue(count==0||count>=2,"A single line of the paragraph was left on page "+page.index());
        }
    }
    @Test void widowControlMovesTheLastTwoLinesAndAvoidsSingleFirstLines(){
        var longParagraph=lines(8,true);var doc=new WordDocument(List.of(longParagraph),SMALL);
        var layout=new WordLayoutEngine().layout(doc);
        assertEquals(6,body(layout.pages().getFirst()).size());assertEquals(2,body(layout.pages().getLast()).size());
        var orphanDoc=new WordDocument(List.of(paragraph("filler",120),lines(4,true)),SMALL);
        assertEquals(1,pageOf(new WordLayoutEngine().layout(orphanDoc),orphanDoc.paragraphStart(1)));
    }
    @Test void continuousSectionsBalancePrecedingTextColumns(){
        var first=paragraph("one",20);var second=paragraph("two",20);
        var third=paragraph("three",20).withSectionBreak(SMALL.withColumns(2,10));
        var doc=new WordDocument(List.of(first,second,third,paragraph("following",20)),section(SMALL,CONTINUOUS,null));
        var layout=new WordLayoutEngine().layout(doc);assertEquals(1,layout.pages().size());
        var rendered=body(layout.pages().getFirst());
        assertTrue(rendered.get(2).x()>rendered.getFirst().x());
        assertEquals(rendered.getFirst().top(),rendered.get(2).top(),0.05);
        assertEquals(rendered.get(1).bottom(),rendered.get(3).top(),0.05);
    }
    @Test void keepsChainsUsingMeasuredParagraphHeights(){
        var filler=paragraph("filler",80);
        var a=paragraph("heading",20).withStyle(paragraph("",20).style().withKeepWithNext(true));
        var b=paragraph("subheading",20).withStyle(a.style());var c=paragraph("content",30);
        var doc=new WordDocument(List.of(filler,a,b,c),SMALL);var layout=new WordLayoutEngine().layout(doc);
        assertEquals(1,pageOf(layout,doc.paragraphStart(1)));assertEquals(1,pageOf(layout,doc.paragraphStart(3)));
    }
    @Test void continuousSectionsStayOnSameSheetAndOrientationChangesStartNewSheet(){
        var first=paragraph("first",20).withSectionBreak(SMALL);
        var last=paragraph("last",20);var continuous=section(SMALL,CONTINUOUS,null);
        assertEquals(1,new WordLayoutEngine().layout(new WordDocument(List.of(first,last),continuous)).pages().size());
        assertEquals(2,new WordLayoutEngine().layout(new WordDocument(List.of(first,last),continuous.withOrientation(false))).pages().size());
        var columns=new WordLayoutEngine().layout(new WordDocument(List.of(first,last),continuous.withColumns(2,10)));
        assertEquals(1,columns.pages().size());assertTrue(body(columns.pages().getFirst()).getLast().width()<SMALL.contentWidth());
    }
    @Test void oddAndEvenBreaksUsePhysicalParityAndRestartDisplayedNumbers(){
        for(var type:List.of(ODD_PAGE,EVEN_PAGE)){
            var doc=new WordDocument(List.of(paragraph("first",20).withSectionBreak(SMALL),paragraph("last",20)),section(SMALL,type,null).withPageNumberStart(7));
            var layout=new WordLayoutEngine().layout(doc);int expected=type==ODD_PAGE?3:2;
            assertEquals(expected,layout.pages().size());assertEquals(7,layout.pages().getLast().number());
            if(type==ODD_PAGE)assertTrue(body(layout.pages().get(1)).isEmpty());
        }
    }
    @Test void headersBelongToSectionsAndLinksSurviveRoundTrip()throws Exception {
        var a=section(SMALL,NEXT_PAGE,"alpha");var b=section(SMALL,NEXT_PAGE,"beta");var c=section(SMALL,NEXT_PAGE,null);
        var doc=new WordDocument(List.of(paragraph("A",20).withSectionBreak(a),paragraph("B",20).withSectionBreak(b),paragraph("C",20)),c);
        var imported=read(write(doc,null));assertEquals(3,imported.document().sections().size());
        assertEquals("alpha",imported.document().headersAt(0).get(WordHeaders.Kind.HEADER).getFirst().plainText());
        assertEquals("beta",imported.document().headersAt(imported.document().paragraphStart(2)).get(WordHeaders.Kind.HEADER).getFirst().plainText());
        assertTrue(imported.document().pageSettings().section().linkedHeaders().contains(WordHeaders.Kind.HEADER));
        var edited=imported.document().replace(0,0,"edited",WordTextStyle.DEFAULT);
        var reopened=read(write(edited,imported));
        for(int i=0;i<3;i++){
            var beforeSection=imported.document().sections().get(i);var afterSection=reopened.document().sections().get(i);
            assertEquals(beforeSection.section().linkedHeaders(),afterSection.section().linkedHeaders());
            assertEquals(beforeSection.section().originalReferences(),afterSection.section().originalReferences());
            assertEquals(beforeSection.width(),afterSection.width());
        }
        var layout=new WordLayoutEngine().layout(reopened.document());
        assertEquals(3,layout.pages().size());assertTrue(layout.pages().get(1).lines().stream().anyMatch(l->l.region()==WordLayout.Region.HEADER));
        var before=OpcPackage.read(imported.originalBytes(),OpcPackage.Limits.DEFAULT);var after=OpcPackage.read(write(edited,imported),OpcPackage.Limits.DEFAULT);
        for(String name:before.names())if(name.contains("header")&&name.endsWith(".xml"))assertArrayEquals(before.part(name),after.part(name));
    }
    @Test void unknownSectionSettingsSurviveAdjacentEditing()throws Exception {
        byte[] bytes=write(new WordDocument(List.of(paragraph("original",20)),SMALL),null);
        bytes=patch(bytes,"word/document.xml","</w:sectPr>","<w:docGrid w:type=\"lines\" w:linePitch=\"360\"/></w:sectPr>");
        bytes=patch(bytes,"word/document.xml","w:gutter=\"0\"","w:gutter=\"240\"");
        var imported=read(bytes);byte[] edited=write(imported.document().replace(0,0,"new ",WordTextStyle.DEFAULT),imported);
        var dom=OoxmlXml.parse(OpcPackage.read(edited,OpcPackage.Limits.DEFAULT).part("word/document.xml"));
        var section=OoxmlXml.path(dom.getDocumentElement(),"body","sectPr");
        assertEquals("240",OoxmlXml.attr(OoxmlXml.child(section,"pgMar"),"gutter"));
        assertEquals("360",OoxmlXml.attr(OoxmlXml.child(section,"docGrid"),"linePitch"));
    }
    @Test void sectionBreakTypesSurviveUnrelatedEdits()throws Exception {
        for(var type:WordSectionProperties.BreakType.values()){
            var doc=new WordDocument(List.of(paragraph("one",20).withSectionBreak(SMALL),paragraph("two",20)),section(SMALL,type,null));
            var imported=read(write(doc,null));assertEquals(type,imported.document().pageSettings().section().breakType());
            var saved=read(write(imported.document().replace(0,0,"edit ",WordTextStyle.DEFAULT),imported));
            assertEquals(type,saved.document().pageSettings().section().breakType());
        }
    }
    @Test void individualColumnWidthsArePreservedAndUnsafeGeometryChangesAreRejected()throws Exception {
        byte[] bytes=write(new WordDocument(List.of(paragraph("columns",20)),SMALL.withColumns(2,10)),null);
        bytes=patch(bytes,"word/document.xml","<w:cols w:space=\"200\" w:num=\"2\"/>",
                "<w:cols w:space=\"200\" w:num=\"2\" w:equalWidth=\"0\"><w:col w:w=\"1200\" w:space=\"200\"/><w:col w:w=\"2800\"/></w:cols>");
        var imported=read(bytes);assertTrue(imported.diagnostics().stream().anyMatch(d->d.contains("larguras individuais")));
        byte[] saved=write(imported.document().replace(0,0,"edit ",WordTextStyle.DEFAULT),imported);
        assertTrue(xml(saved,"word/document.xml").contains("w:w=\"1200\""));
        assertThrows(IOException.class,()->write(imported.document().withPageSettings(imported.document().pageSettings().withColumns(1,0)),imported));
    }
    @Test void sectionGeometryCanBeInheritedFromFollowingSection()throws Exception {
        var doc=new WordDocument(List.of(paragraph("first",20).withSectionBreak(SMALL),paragraph("following",20)),section(SMALL,CONTINUOUS,null));
        var bytes=write(doc,null);var archive=OpcPackage.read(bytes,OpcPackage.Limits.DEFAULT);
        var dom=OoxmlXml.parse(archive.part("word/document.xml"));var section=OoxmlXml.path(dom.getDocumentElement(),"body","p","pPr","sectPr");
        section.removeChild(OoxmlXml.child(section,"pgSz"));section.removeChild(OoxmlXml.child(section,"pgMar"));
        var out=new ByteArrayOutputStream();archive.withPart("word/document.xml",OoxmlXml.bytes(dom)).write(out);
        var imported=read(out.toByteArray());assertEquals(SMALL.width(),imported.document().sections().getFirst().width());
        assertEquals(SMALL.top(),imported.document().sections().getFirst().top());
    }
    @Test void explicitEmptyHeadersDoNotInheritPreviousContent()throws Exception {
        var a=section(SMALL,NEXT_PAGE,"previous");
        var empty=SMALL.withSection(WordSectionProperties.DEFAULT.withLinked(WordHeaders.Kind.HEADER,false));
        var doc=new WordDocument(List.of(paragraph("A",20).withSectionBreak(a),paragraph("B",20)),empty);
        var reopened=read(write(doc,null)).document();
        assertFalse(reopened.pageSettings().section().linkedHeaders().contains(WordHeaders.Kind.HEADER));
        assertTrue(reopened.headersAt(reopened.paragraphStart(1)).get(WordHeaders.Kind.HEADER).stream().allMatch(p->p.plainText().isEmpty()));
    }
    @Test void legacyGlobalHeadersWorkWithSharedSectionSettingsInstances()throws Exception {
        var headers=WordHeaders.EMPTY.with(WordHeaders.Kind.HEADER,List.of(paragraph("legacy",12)));
        var doc=new WordDocument(List.of(paragraph("first",20).withSectionBreak(SMALL),paragraph("last",20)),SMALL,WordParts.EMPTY.withHeaders(headers));
        var imported=read(write(doc,null)).document();
        assertEquals("legacy",imported.headersAt(0).get(WordHeaders.Kind.HEADER).getFirst().plainText());
        assertEquals("legacy",imported.headersAt(imported.paragraphStart(1)).get(WordHeaders.Kind.HEADER).getFirst().plainText());
        assertTrue(imported.pageSettings().section().linkedHeaders().contains(WordHeaders.Kind.HEADER));
    }
    @Test void firstAndEvenHeadersRemainIndependentAcrossSections()throws Exception {
        var headers=WordHeaders.EMPTY.with(WordHeaders.Kind.HEADER,List.of(paragraph("normal",12)))
                .with(WordHeaders.Kind.FIRST_HEADER,List.of(paragraph("first",12)))
                .with(WordHeaders.Kind.EVEN_HEADER,List.of(paragraph("even",12))).withOptions(true,true);
        var props=WordSectionProperties.DEFAULT.withHeaders(headers);
        for(var kind:List.of(WordHeaders.Kind.HEADER,WordHeaders.Kind.FIRST_HEADER,WordHeaders.Kind.EVEN_HEADER))props=props.withLinked(kind,false);
        var doc=new WordDocument(List.of(lines(2,false)),WordPageSettings.A4.withSection(props),WordParts.EMPTY.withHeaders(WordHeaders.EMPTY.withOptions(false,true)));
        var imported=read(write(doc,null));var resolved=imported.document().headersAt(0);
        assertEquals("first",resolved.header(1,true).getFirst().plainText());
        assertEquals("even",resolved.header(2,false).getFirst().plainText());
        assertEquals("normal",resolved.header(3,false).getFirst().plainText());
        var saved=read(write(imported.document().replace(0,0,"edit",WordTextStyle.DEFAULT),imported));
        assertTrue(saved.document().pageSettings().section().headers().differentFirst());assertTrue(saved.document().parts().headers().differentOddEven());
    }
    @Test void headerImagesAndRelationshipsArePreservedWhenOnlyOptionsChange()throws Exception {
        var png=new ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",png);
        var resource=WordResource.of(png.toByteArray(),"image/png");
        var header=paragraph("",12).withRuns(List.of(new WordObjectRun(WordImage.of(resource.id(),10,10),WordTextStyle.DEFAULT)));
        var settings=SMALL.withSection(WordSectionProperties.DEFAULT.withHeaders(WordHeaders.EMPTY.with(WordHeaders.Kind.HEADER,List.of(header)))
                .withLinked(WordHeaders.Kind.HEADER,false));
        var imported=read(write(new WordDocument(List.of(paragraph("body",20).withSectionBreak(settings),paragraph("linked",20)),SMALL,
                WordParts.EMPTY.withResources(WordResources.EMPTY.with(resource))),null));
        var session=new WordSession();session.load(imported.document());var controller=new WordDocumentController(session);
        controller.applyHeaderFooter(WordHeaders.Kind.HEADER,"",WordParagraphStyle.Alignment.CENTER,false,true,false,false,false);
        var saved=read(write(session.getDocument(),imported));
        assertEquals(1,saved.document().parts().resources().size());
        assertEquals(1,saved.document().usedResourceIds().size());
        assertTrue(((WordParagraph)saved.document().headersAt(0).get(WordHeaders.Kind.HEADER).getFirst()).runs().getFirst() instanceof WordObjectRun);
        assertThrows(IllegalStateException.class,()->controller.setHeaderFooterText(WordHeaders.Kind.HEADER,"replace",WordParagraphStyle.Alignment.LEFT,false));
        int next=session.getDocument().paragraphStart(1);session.setSelection(next,next);
        controller.applyHeaderFooter(WordHeaders.Kind.HEADER,"",WordParagraphStyle.Alignment.CENTER,false,false,false,false,false);
        var unlinked=read(write(session.getDocument(),imported));
        assertFalse(unlinked.document().pageSettings().section().linkedHeaders().contains(WordHeaders.Kind.HEADER));
        assertEquals(imported.document().sections().getFirst().section().originalReferences().get(WordHeaders.Kind.HEADER),
                unlinked.document().pageSettings().section().originalReferences().get(WordHeaders.Kind.HEADER));
    }
    @Test void headerFormChangesAreAtomicUndoableAndReadOnlyIsEnforced()throws Exception {
        WordEditorTest.edt(()->{
            var session=new WordSession();session.load(new WordDocument(List.of(paragraph("A",20).withSectionBreak(SMALL),paragraph("B",20)),SMALL));
            session.setSelection(2,2);var controller=new WordDocumentController(session);var initial=session.getDocument();
            controller.applyHeaderFooter(WordHeaders.Kind.HEADER,"new",WordParagraphStyle.Alignment.CENTER,true,true,true,false,true);
            var changed=session.getDocument();assertFalse(changed.pageSettings().section().linkedHeaders().contains(WordHeaders.Kind.HEADER));
            session.undo();assertEquals(initial,session.getDocument());session.redo();assertEquals(changed,session.getDocument());
            session.setReadOnly(true);assertThrows(IllegalStateException.class,()->controller.setHeaderOptions(false,false));
            return null;
        });
    }
    @Test void formsKeepUntouchedPropertiesAndRichHeaders()throws Exception {
        WordEditorTest.edt(()->{
            var settings=section(WordPageSettings.A4,ODD_PAGE,"custom").withPageNumberStart(42);
            assertEquals(settings,new WordPageSetupPanel(settings).result());
            var style=WordParagraphStyle.DEFAULT.withLineSpacing(EXACT,18).withKeepLines(true).withWidowControl(false);
            assertEquals(style,new WordParagraphPropertiesPanel(style).result());
            var headers=WordHeaders.EMPTY.with(WordHeaders.Kind.HEADER,List.of(WordParagraph.of("keep formatting",WordTextStyle.DEFAULT.withBold(true),style)));
            assertFalse(new WordHeaderFooterPanel(headers,false).result().textChanged());
            return null;
        });
    }
}
