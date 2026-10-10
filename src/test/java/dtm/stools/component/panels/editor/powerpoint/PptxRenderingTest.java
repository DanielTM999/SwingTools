package dtm.stools.component.panels.editor.powerpoint;

import dtm.stools.component.panels.editor.powerpoint.io.PptxCodec;
import dtm.stools.component.panels.editor.powerpoint.model.*;
import dtm.stools.component.panels.editor.powerpoint.render.*;
import dtm.stools.component.panels.editor.powerpoint.ui.PptInlineTextEditor;
import dtm.stools.component.panels.editor.word.io.ooxml.*;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.Color;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PptxRenderingTest {
    private final PptxCodec codec=new PptxCodec();
    private PptText rich(){return new PptText(List.of(new PptText.Paragraph(List.of(
            new PptText.Run("Bold Arial ",new PptText.Style("Arial",32,true,false,false,Color.BLUE)),
            new PptText.Run("italic and underlined",new PptText.Style("Arial",24,false,true,true,Color.RED))),"ctr",0,0,1)),0,0,0,0,"ctr");}
    private PptxCodec.ImportResult external(Presentation deck)throws Exception {
        ByteArrayOutputStream output=new ByteArrayOutputStream();codec.write(deck,output);OpcPackage own=OpcPackage.read(output.toByteArray(),OpcPackage.Limits.DEFAULT);Map<String,byte[]> parts=new LinkedHashMap<>();
        for(String name:own.names())if(!name.equals("ppt/swingtools.xml"))parts.put(name,own.part(name));parts.put("ppt/unknown.xml","<unknown keep=\"yes\"/>".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream converted=new ByteArrayOutputStream();new OpcPackage(parts).write(converted);return codec.read(new ByteArrayInputStream(converted.toByteArray()));
    }
    private byte[] save(Presentation deck,PptxCodec.ImportResult origin)throws Exception {ByteArrayOutputStream output=new ByteArrayOutputStream();codec.write(deck,origin,output);return output.toByteArray();}
    @Test
    void nativePlainTextUsesConsistentFontUnitsInExternalReaders()throws Exception {
        PptObject text=PptObject.text("Plain text",40,50,300,100);var read=external(Presentation.create().withSlide(0,PptSlide.create("Plain").addObject(text))).presentation().slides().getFirst().objects().getFirst();
        assertEquals(text.fontSize(),read.styledText().firstStyle().size(),.01);assertEquals(6,read.styledText().left(),.001);
    }
    @Test
    void legacyMetadataRemainsEditableWithoutRewritingOnOpen()throws Exception {
        Presentation deck=Presentation.create().withSlide(0,PptSlide.create("Legacy").addObject(PptObject.text("Legacy text",40,50,300,100)));
        OpcPackage modern=OpcPackage.read(save(deck,null),OpcPackage.Limits.DEFAULT);Map<String,byte[]> parts=new LinkedHashMap<>();for(String name:modern.names())parts.put(name,modern.part(name));
        String metadata=new String(parts.get("ppt/swingtools.xml"),StandardCharsets.UTF_8).replace(" version=\"2\"","");parts.put("ppt/swingtools.xml",metadata.getBytes(StandardCharsets.UTF_8));
        String xml=new String(parts.get("ppt/slides/slide1.xml"),StandardCharsets.UTF_8).replaceAll("<a:bodyPr[^>]*/>","<a:bodyPr/>").replace("sz=\"2025\"","sz=\"3600\"");parts.put("ppt/slides/slide1.xml",xml.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();new OpcPackage(parts).write(bytes);var imported=codec.read(new ByteArrayInputStream(bytes.toByteArray()));assertTrue(imported.editable());assertArrayEquals(bytes.toByteArray(),save(imported.presentation(),imported));
    }
    @Test
    void styledTextAndTablesNativeRoundTrip()throws Exception {
        PptObject text=PptObject.text("",40,50,600,120).withStyledText(rich());PptObject table=PptObject.table(3,3,70,250,600,240);table=table.withTable(table.visual().table().merge(1,1,2,2));
        PptObject rounded=PptObject.shape(PptObject.Kind.ROUND_RECTANGLE,800,80,220,140).withStyledText(rich());
        Presentation deck=Presentation.create().withSlide(0,PptSlide.create("Rich").addObject(text).addObject(table).addObject(rounded).addObject(PptObject.connector(300,500,300,650)));
        byte[] bytes=save(deck,null);PptxCodec.ImportResult read=codec.read(new ByteArrayInputStream(bytes));assertTrue(read.editable());
        assertEquals(rich(),read.presentation().slides().getFirst().objects().getFirst().styledText());
        assertEquals(2,read.presentation().slides().getFirst().objects().get(1).visual().table().rows().get(1).get(1).rowSpan());
        assertArrayEquals(bytes,save(read.presentation(),read));
        assertEquals(0,read.presentation().slides().getFirst().objects().getLast().visual().connector().x2());
    }
    @Test
    void externalGeometryUsesActualSlideWidth()throws Exception {
        PptObject text=PptObject.text("",80,60,400,150).withStyledText(rich());var origin=external(Presentation.create().withSlide(0,PptSlide.create("Geometry").addObject(text)));
        OpcPackage original=OpcPackage.read(origin.originalBytes(),OpcPackage.Limits.DEFAULT);Map<String,byte[]> parts=new LinkedHashMap<>();for(String name:original.names())parts.put(name,original.part(name));
        var presentation=OoxmlXml.parse(parts.get("ppt/presentation.xml"));var size=OoxmlXml.descendant(presentation.getDocumentElement(),"sldSz");size.setAttribute("cx","12192000");size.setAttribute("cy","6858000");parts.put("ppt/presentation.xml",OoxmlXml.bytes(presentation));
        var slide=OoxmlXml.parse(parts.get("ppt/slides/slide1.xml"));
        for(String tag:List.of("off","ext"))for(var e:OoxmlXml.descendants(slide.getDocumentElement(),tag))for(String key:List.of("x","y","cx","cy"))if(e.hasAttribute(key))e.setAttribute(key,Long.toString(Math.round(Double.parseDouble(e.getAttribute(key))*4/3)));
        parts.put("ppt/slides/slide1.xml",OoxmlXml.bytes(slide));ByteArrayOutputStream output=new ByteArrayOutputStream();new OpcPackage(parts).write(output);var actual=codec.read(new ByteArrayInputStream(output.toByteArray())).presentation().slides().getFirst().objects().getFirst();
        assertEquals(80,actual.x(),.001);assertEquals(60,actual.y(),.001);assertEquals(400,actual.width(),.001);assertEquals(24,actual.styledText().firstStyle().size(),.001);
    }
    @Test
    void externalInsertionDeletionAndRepeatedSavesPreserveUnknownParts()throws Exception {
        Presentation deck=Presentation.create().withSlide(0,PptSlide.create("External").addObject(PptObject.text("",40,50,600,140).withStyledText(rich())).addObject(PptObject.table(2,3,80,240,600,200)));
        var origin=external(deck);PptSlide first=origin.presentation().slides().getFirst();PptObject originalTable=first.objects().get(1);
        var table=originalTable.visual().table();table=table.cell(0,0,table.rows().getFirst().getFirst().withText(rich())).insertRow(1).insertColumn(1);
        PptObject added=PptObject.shape(PptObject.Kind.DIAMOND,850,200,120,120).withStyledText(rich());
        Presentation changed=origin.presentation().withSlide(0,first.replaceObject(originalTable.id(),originalTable.withTable(table)).addObject(added).addObject(PptObject.connector(100,600,500,600)));
        assertTrue(codec.validateEdit(origin,origin.presentation(),changed).isEmpty());var saved=codec.read(new ByteArrayInputStream(save(changed,origin)));assertTrue(saved.editable());assertEquals(4,saved.presentation().slides().getFirst().objects().size());
        assertTrue(saved.presentation().slides().getFirst().objects().stream().anyMatch(o->o.id().equals(added.id())));
        PptSlide reopened=saved.presentation().slides().getFirst();Presentation again=saved.presentation().withSlide(0,reopened.removeObject(added.id()));byte[] second=save(again,saved);
        var packageData=OpcPackage.read(second,OpcPackage.Limits.DEFAULT);assertArrayEquals("<unknown keep=\"yes\"/>".getBytes(StandardCharsets.UTF_8),packageData.part("ppt/unknown.xml"));
        assertEquals(3,codec.read(new ByteArrayInputStream(second)).presentation().slides().getFirst().objects().size());
        assertArrayEquals( origin.originalBytes(), save(origin.presentation(),origin));
    }
    @Test
    void textWrapsAndAllRenderingSizesUseSameLayout(){
        var renderer=new PowerPointRenderer();PptText text=PptText.plain("A long sentence that needs several lines within a narrow box.",24,Color.BLACK);
        var layout=renderer.textLayout().layout(text,180,240);assertTrue(layout.lines().size()>2);assertTrue(layout.height()<=240);
        for(var line:layout.lines())assertTrue(line.text().getAdvance()<=168.01);
        PptObject object=PptObject.text("",40,50,180,240).withStyledText(text);var deck=Presentation.create().withSlide(0,PptSlide.create("Wrap").addObject(object));
        for(int width:new int[]{320,640,1280}){BufferedImage image=new BufferedImage(width,width*9/16,BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();renderer.render(graphics,deck,deck.slides().getFirst(),new Rectangle2D.Double(0,0,image.getWidth(),image.getHeight()),Map.of());graphics.dispose();}
        assertSame(layout,renderer.textLayout().layout(text,180,240));
    }
    @Test
    void inlineEditorKeepsRunsAndCancelsOrCommitsAtomically()throws Exception {
        SwingUtilities.invokeAndWait(()->{
            PptInlineTextEditor input=new PptInlineTextEditor(rich(),.5);assertEquals(rich(),input.value());input.select(0,4);input.format(s->s.color(Color.GREEN));
            assertEquals(Color.GREEN,input.value().paragraphs().getFirst().runs().getFirst().style().color());assertTrue(input.value().paragraphs().getFirst().runs().getFirst().style().bold());
            try(PowerPointEditor editor=new PowerPointEditor()){
                editor.insertText("Original");PptObject o=editor.getPresentation().slides().getFirst().objects().getFirst();editor.getCanvas().setSize(900,600);editor.getCanvas().paint(new BufferedImage(900,600,BufferedImage.TYPE_INT_RGB).getGraphics());
                editor.getCanvas().startEditing(o);editor.getCanvas().cancelEditing();assertEquals("Original",editor.getPresentation().slides().getFirst().objects().getFirst().text());
                editor.getCanvas().startEditing(o);PptInlineTextEditor active=(PptInlineTextEditor)editor.getCanvas().getComponent(0);active.setText("Changed");editor.getCanvas().commitEditing();assertEquals("Changed",editor.getPresentation().slides().getFirst().objects().getFirst().text());editor.undo();assertEquals("Original",editor.getPresentation().slides().getFirst().objects().getFirst().text());
            }
        });
    }
    @Test
    void connectorTracksMovedTargetAndIsDetachedOnRemoval(){
        PptObject target=PptObject.shape(PptObject.Kind.DIAMOND,100,100,100,100),line=PptObject.connector(150,100,500,100);var c=line.visual().connector();line=line.withVisual(line.visual().withConnector(new PptVisual.Connector(c.x1(),c.y1(),c.x2(),c.y2(),target.id(),null,0,0)));
        PptSlide slide=PptSlide.create("Connections").addObject(target).addObject(line);var moved=slide.replaceObject(target.id(),target.geometry(200,200,100,100));PptObject connector=moved.objects().getLast();assertEquals(250,connector.x()+connector.visual().connector().x1());assertEquals(200,connector.y()+connector.visual().connector().y1());
        assertNull(moved.removeObject(target.id()).objects().getFirst().visual().connector().startId());assertNull(connector.duplicate().visual().connector().startId());
        assertTrue(new PowerPointRenderer().hit(PptObject.connector(200,200,200,500),new Point2D.Double(201,300),5));
    }
    @Test
    void tableStructuralChangesKeepGridAndMergedCellsValid(){
        PptTable table=PptTable.create(3,3,600,240).merge(0,0,2,2);assertTrue(table.rows().get(1).get(1).covered());assertEquals(2,table.rows().getFirst().getFirst().colSpan());
        table=table.split().insertRow(1).insertColumn(2).deleteRow(0).deleteColumn(0);assertEquals(3,table.rows().size());assertEquals(3,table.columns().size());
        assertThrows(IllegalArgumentException.class,()->PptTable.create(0,2,100,100));
        PptTable merged=PptTable.create(3,3,600,240).merge(0,0,2,2).insertRow(1).insertColumn(1);
        assertEquals(3,merged.rows().getFirst().getFirst().rowSpan());assertEquals(3,merged.rows().getFirst().getFirst().colSpan());
        merged=merged.deleteRow(0).deleteColumn(0);assertEquals(2,merged.rows().getFirst().getFirst().rowSpan());assertEquals(2,merged.rows().getFirst().getFirst().colSpan());
    }
    @Test
    void inheritedGeometryThemeFontsAndMasterStylesAreResolved()throws Exception {
        var origin=external(Presentation.create().withSlide(0,PptSlide.create("Inheritance").addObject(PptObject.text("Title",80,60,500,160))));
        OpcPackage pkg=OpcPackage.read(origin.originalBytes(),OpcPackage.Limits.DEFAULT);Map<String,byte[]> parts=new LinkedHashMap<>();for(String name:pkg.names())parts.put(name,pkg.part(name));
        var slide=OoxmlXml.parse(parts.get("ppt/slides/slide1.xml"));var shape=OoxmlXml.child(OoxmlXml.path(slide.getDocumentElement(),"cSld","spTree"),"sp");
        var ph=slide.createElementNS("http://schemas.openxmlformats.org/presentationml/2006/main","p:ph");ph.setAttribute("type","title");OoxmlXml.path(shape,"nvSpPr","nvPr").appendChild(ph);
        var master=OoxmlXml.parse(parts.get("ppt/slideMasters/slideMaster1.xml"));OoxmlXml.path(master.getDocumentElement(),"cSld","spTree").appendChild(master.importNode(shape,true));
        var title=OoxmlXml.path(master.getDocumentElement(),"txStyles","titleStyle");var styles=OoxmlXml.parse(("<a:lstStyle xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\"><a:lvl1pPr><a:defRPr sz=\"2700\" b=\"1\"><a:solidFill><a:schemeClr val=\"accent1\"/></a:solidFill><a:latin typeface=\"+mj-lt\"/></a:defRPr></a:lvl1pPr></a:lstStyle>").getBytes(StandardCharsets.UTF_8));title.appendChild(master.importNode(OoxmlXml.child(styles.getDocumentElement(),"lvl1pPr"),true));
        var props=OoxmlXml.child(shape,"spPr");props.removeChild(OoxmlXml.child(props,"xfrm"));for(var run:OoxmlXml.descendants(shape,"r"))run.removeChild(OoxmlXml.child(run,"rPr"));
        var layout=OoxmlXml.parse(parts.get("ppt/slideLayouts/slideLayout1.xml"));OoxmlXml.path(layout.getDocumentElement(),"cSld","spTree").appendChild(layout.importNode(shape,true));
        var theme=OoxmlXml.parse(parts.get("ppt/theme/theme1.xml"));OoxmlXml.path(OoxmlXml.descendant(theme.getDocumentElement(),"fontScheme"),"majorFont","latin").setAttribute("typeface","Georgia");
        parts.put("ppt/slides/slide1.xml",OoxmlXml.bytes(slide));parts.put("ppt/slideMasters/slideMaster1.xml",OoxmlXml.bytes(master));parts.put("ppt/slideLayouts/slideLayout1.xml",OoxmlXml.bytes(layout));parts.put("ppt/theme/theme1.xml",OoxmlXml.bytes(theme));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();new OpcPackage(parts).write(bytes);PptObject read=codec.read(new ByteArrayInputStream(bytes.toByteArray())).presentation().slides().getFirst().objects().getFirst();
        assertEquals(80,read.x());assertEquals(60,read.y());assertEquals(48,read.styledText().firstStyle().size(),.001);assertEquals("Georgia",read.styledText().firstStyle().family());assertTrue(read.styledText().firstStyle().bold());assertEquals(new Color(0x3474D2),read.styledText().firstStyle().color());
    }
    @Test
    void insertedImagesAndConnectorAttachmentsSurviveExternalSave()throws Exception {
        var origin=external(Presentation.create());PptObject target=PptObject.shape(PptObject.Kind.RECTANGLE,200,100,200,100),line=PptObject.connector(300,100,700,100);var c=line.visual().connector();line=line.withVisual(line.visual().withConnector(new PptVisual.Connector(c.x1(),c.y1(),c.x2(),c.y2(),target.id(),null,0,0)));
        ByteArrayOutputStream imageBytes=new ByteArrayOutputStream();javax.imageio.ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",imageBytes);
        var changed=origin.presentation().withSlide(0,origin.presentation().slides().getFirst().addObject(target).addObject(line).addObject(PptObject.media(PptObject.Kind.IMAGE,"image/png",imageBytes.toByteArray(),30,40,100,80)));
        var read=codec.read(new ByteArrayInputStream(save(changed,origin)));var objects=read.presentation().slides().getFirst().objects();assertEquals(target.id(),objects.get(1).visual().connector().startId());assertArrayEquals(imageBytes.toByteArray(),objects.getLast().data());assertEquals(30,objects.getLast().x(),.001);assertEquals(40,objects.getLast().y(),.001);
        var moved=read.presentation().withSlide(0,read.presentation().slides().getFirst().replaceObject(target.id(),objects.getFirst().geometry(400,300,200,100)));var again=codec.read(new ByteArrayInputStream(save(moved,read)));var link=again.presentation().slides().getFirst().objects().get(1);assertEquals(500,link.x()+link.visual().connector().x1(),.001);
    }
}
