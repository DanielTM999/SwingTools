package dtm.stools.component.panels.editor.powerpoint;

import dtm.stools.component.panels.editor.powerpoint.api.PowerPointSession;
import dtm.stools.component.panels.editor.powerpoint.io.PptxCodec;
import dtm.stools.component.panels.editor.powerpoint.model.*;
import dtm.stools.component.panels.editor.powerpoint.provider.PowerPointCommandProvider;
import dtm.stools.component.panels.editor.powerpoint.provider.PowerPointDialogProvider;
import dtm.stools.component.panels.editor.powerpoint.provider.PowerPointFileDialogProvider;
import dtm.stools.component.panels.editor.powerpoint.ui.PowerPointRibbon;
import dtm.stools.component.panels.editor.word.io.ooxml.OpcPackage;
import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.SwingUtilities;
import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class PowerPointEditorTest {
    @Test void chooseOpenConfirmsBeforeReplacingUnsavedExample(@TempDir Path folder)throws Exception {
        Path file=folder.resolve("opened.pptx");
        Presentation incoming=Presentation.create().withSlide(0,Presentation.create().slides().getFirst()
                .addObject(PptObject.text("From file",10,10,200,80)));
        try(OutputStream output=Files.newOutputStream(file)){new PptxCodec().write(incoming,output);}
        AtomicReference<PowerPointEditor> holder=new AtomicReference<>();
        AtomicBoolean allowDiscard=new AtomicBoolean();
        AtomicInteger confirmations=new AtomicInteger(),pickerCalls=new AtomicInteger();
        CountDownLatch opened=new CountDownLatch(1);
        SwingUtilities.invokeAndWait(()->{
            PowerPointEditor editor=new PowerPointEditor();holder.set(editor);
            editor.insertText("Unsaved example");
            editor.getSession().addListener(()->{if(!editor.getSession().isDirty())opened.countDown();});
            editor.addProvider(new PowerPointDialogProvider(){
                @Override public String id(){return "test.confirm";}
                @Override public Optional<String> editText(Component owner,String current){return Optional.empty();}
                @Override public Optional<Color> chooseColor(Component owner,String title,Color current){return Optional.empty();}
                @Override public boolean confirmDiscardChanges(Component owner){confirmations.incrementAndGet();return allowDiscard.get();}
            });
            editor.addProvider(new PowerPointFileDialogProvider(){
                @Override public String id(){return "test.files";}
                @Override public Optional<Path> chooseOpen(Component owner){pickerCalls.incrementAndGet();return Optional.of(file);}
                @Override public Optional<Path> chooseSave(Component owner,Path current){return Optional.empty();}
                @Override public Optional<Path> chooseMedia(Component owner,PptObject.Kind kind){return Optional.empty();}
            });
        });
        PowerPointEditor editor=holder.get();
        try{
            SwingUtilities.invokeAndWait(()->{
                assertThrows(IllegalStateException.class,()->editor.open(file));
                editor.chooseOpen();
                assertEquals(1,confirmations.get());assertEquals(0,pickerCalls.get());
                assertTrue(editor.getSession().isDirty());
            });
            allowDiscard.set(true);
            SwingUtilities.invokeAndWait(editor::chooseOpen);
            assertTrue(opened.await(10,TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(()->{
                assertEquals(2,confirmations.get());assertEquals(1,pickerCalls.get());
                assertEquals("From file",editor.getPresentation().slides().getFirst().objects().getFirst().text());
                assertFalse(editor.getSession().isDirty());
            });
        }finally{SwingUtilities.invokeAndWait(editor::close);}
    }
    @Test void sessionHistoryAndSelection(){
        PowerPointSession session=new PowerPointSession();
        PptObject text=PptObject.text("Hello",10,20,300,80);
        session.edit(doc->doc.withSlide(0,doc.slides().getFirst().addObject(text)));
        assertTrue(session.isDirty());assertEquals(1,session.getPresentation().slides().getFirst().objects().size());
        session.undo();assertTrue(session.getPresentation().slides().getFirst().objects().isEmpty());
        session.redo();assertEquals(text.id(),session.getPresentation().slides().getFirst().objects().getFirst().id());
        session.selectObject(text.id());assertEquals(text.id(),session.selectedObjectId());
    }
    @Test void pptxRoundTripAndMediaRelationship()throws Exception {
        PptObject image=PptObject.media(PptObject.Kind.IMAGE,"image/png",new byte[]{1,2,3},20,30,200,100);
        PptSlide second=PptSlide.create("Second").addObject(image).withTransition("fade");
        second=second.withAnimations(List.of(PptAnimation.create(image.id(),PptAnimation.Effect.FADE_IN)));
        Presentation original=Presentation.create().insertSlide(1,second);
        PptxCodec codec=new PptxCodec();ByteArrayOutputStream output=new ByteArrayOutputStream();codec.write(original,output);
        byte[] bytes=output.toByteArray();OpcPackage packageData=OpcPackage.read(bytes,OpcPackage.Limits.DEFAULT);
        assertTrue(packageData.contains("ppt/slides/slide2.xml"));
        assertTrue(new String(packageData.part("ppt/slides/_rels/slide2.xml.rels")).contains("../media/image2_2.png"));
        assertTrue(packageData.contains("ppt/media/image2_2.png"));
        PptxCodec.ImportResult read=codec.read(new ByteArrayInputStream(bytes));
        assertTrue(read.editable());assertEquals(2,read.presentation().slides().size());
        assertEquals("Second",read.presentation().slides().get(1).title());
        assertEquals(PptAnimation.Effect.FADE_IN,read.presentation().slides().get(1).animations().getFirst().effect());
        assertArrayEquals(image.data(),read.presentation().slides().get(1).objects().getFirst().data());
        ByteArrayOutputStream unchanged=new ByteArrayOutputStream();codec.write(read.presentation(),read,unchanged);
        assertArrayEquals(bytes,unchanged.toByteArray());
        String timing=new String(packageData.part("ppt/slides/slide2.xml"));
        assertTrue(timing.contains("<p:timing>"));
        assertTrue(timing.contains("<p:animEffect transition=\"in\" filter=\"fade\""));
    }
    @Test void nativeMediaPartsAndTimingAreLinked()throws Exception {
        PptObject audio=PptObject.media(PptObject.Kind.AUDIO,"audio/wav",new byte[]{1,2,3},10,10,100,100);
        PptObject video=PptObject.media(PptObject.Kind.VIDEO,"video/x-msvideo",new byte[]{4,5,6},120,10,300,180);
        Presentation deck=Presentation.create().withSlide(0,Presentation.create().slides().getFirst().addObject(audio).addObject(video));
        ByteArrayOutputStream output=new ByteArrayOutputStream();new PptxCodec().write(deck,output);
        OpcPackage pkg=OpcPackage.read(output.toByteArray(),OpcPackage.Limits.DEFAULT);
        assertArrayEquals(audio.data(),pkg.part("ppt/media/media1_2.wav"));
        assertArrayEquals(video.data(),pkg.part("ppt/media/media1_3.avi"));
        String slide=new String(pkg.part("ppt/slides/slide1.xml"));
        String rels=new String(pkg.part("ppt/slides/_rels/slide1.xml.rels"));
        assertTrue(slide.contains("<a:audioFile r:link=\"rId2a\""));
        assertTrue(slide.contains("<a:videoFile r:link=\"rId3a\""));
        assertTrue(slide.contains("<p:audio>"));assertTrue(slide.contains("<p:video>"));
        assertTrue(rels.contains("../media/media1_2.wav"));assertTrue(rels.contains("../media/media1_3.avi"));
        assertTrue(new PptxCodec().read(new ByteArrayInputStream(output.toByteArray())).editable());
        for(String name:pkg.names())if(name.endsWith(".xml")||name.endsWith(".rels"))OoxmlXml.parse(pkg.part(name));
        Map<String,byte[]> externalParts=new LinkedHashMap<>();
        for(String name:pkg.names())if(!name.equals("ppt/swingtools.xml"))externalParts.put(name,pkg.part(name));
        ByteArrayOutputStream externalBytes=new ByteArrayOutputStream();new OpcPackage(externalParts).write(externalBytes);
        PptxCodec.ImportResult external=new PptxCodec().read(new ByteArrayInputStream(externalBytes.toByteArray()));
        assertFalse(external.editable());
        assertEquals(PptObject.Kind.AUDIO,external.presentation().slides().getFirst().objects().get(0).kind());
        assertEquals(PptObject.Kind.VIDEO,external.presentation().slides().getFirst().objects().get(1).kind());
        assertArrayEquals(audio.data(),external.presentation().slides().getFirst().objects().get(0).data());
        assertArrayEquals(video.data(),external.presentation().slides().getFirst().objects().get(1).data());
    }
    @Test void nativeAnimationImportsWithoutPrivateMetadata()throws Exception {
        PptObject shape=PptObject.text("Animated",10,10,300,100);
        PptAnimation animation=new PptAnimation("a",shape.id(),PptAnimation.Effect.APPEAR,
                PptAnimation.Start.AFTER_PREVIOUS,850,120,3,"default");
        PptSlide slide=Presentation.create().slides().getFirst().addObject(shape).withAnimations(List.of(animation));
        ByteArrayOutputStream output=new ByteArrayOutputStream();new PptxCodec().write(Presentation.create().withSlide(0,slide),output);
        OpcPackage pkg=OpcPackage.read(output.toByteArray(),OpcPackage.Limits.DEFAULT);
        Map<String,byte[]> parts=new LinkedHashMap<>();for(String name:pkg.names())if(!name.equals("ppt/swingtools.xml"))parts.put(name,pkg.part(name));
        ByteArrayOutputStream external=new ByteArrayOutputStream();new OpcPackage(parts).write(external);
        PptxCodec.ImportResult imported=new PptxCodec().read(new ByteArrayInputStream(external.toByteArray()));
        assertFalse(imported.editable());
        PptAnimation value=imported.presentation().slides().getFirst().animations().getFirst();
        assertEquals(PptAnimation.Effect.APPEAR,value.effect());assertEquals(PptAnimation.Start.AFTER_PREVIOUS,value.start());
        assertEquals(850,value.durationMs());assertEquals(120,value.delayMs());assertEquals(3,value.repeat());
    }
    @Test void externalPptxPatchesKnownShapeAndPreservesUnknownParts()throws Exception {
        PptxCodec codec=new PptxCodec();ByteArrayOutputStream output=new ByteArrayOutputStream();
        Presentation deck=Presentation.create().withSlide(0,Presentation.create().slides().getFirst().addObject(PptObject.text("Original",40,50,300,90)));
        codec.write(deck,output);
        OpcPackage original=OpcPackage.read(output.toByteArray(),OpcPackage.Limits.DEFAULT);
        Map<String,byte[]> parts=new LinkedHashMap<>();for(String name:original.names())if(!name.equals("ppt/swingtools.xml"))parts.put(name,original.part(name));
        parts.put("ppt/unknown.xml","<unknown/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ByteArrayOutputStream external=new ByteArrayOutputStream();new OpcPackage(parts).write(external);
        PptxCodec.ImportResult read=codec.read(new ByteArrayInputStream(external.toByteArray()));
        assertTrue(read.editable());
        ByteArrayOutputStream unchanged=new ByteArrayOutputStream();codec.write(read.presentation(),read,unchanged);
        assertArrayEquals(external.toByteArray(),unchanged.toByteArray());
        PptSlide slide=read.presentation().slides().getFirst();PptObject object=slide.objects().getFirst();
        Presentation changed=read.presentation().withSlide(0,slide.replaceObject(object.id(),object.withText("Alterado")));
        ByteArrayOutputStream rewritten=new ByteArrayOutputStream();codec.write(changed,read,rewritten);
        OpcPackage patched=OpcPackage.read(rewritten.toByteArray(),OpcPackage.Limits.DEFAULT);
        assertArrayEquals(parts.get("ppt/unknown.xml"),patched.part("ppt/unknown.xml"));
        assertArrayEquals(parts.get("ppt/presentation.xml"),patched.part("ppt/presentation.xml"));
        assertTrue(new String(patched.part("ppt/slides/slide1.xml")).contains("Alterado"));
        Presentation structural=read.presentation().insertSlide(1,PptSlide.create("Extra"));
        assertThrows(IOException.class,()->codec.write(structural,read,new ByteArrayOutputStream()));
    }
    @Test void editedXmlOutsideKnownModelProtectsOriginal()throws Exception {
        PptxCodec codec=new PptxCodec();ByteArrayOutputStream output=new ByteArrayOutputStream();codec.write(Presentation.create(),output);
        OpcPackage original=OpcPackage.read(output.toByteArray(),OpcPackage.Limits.DEFAULT);
        Map<String,byte[]> parts=new LinkedHashMap<>();for(String name:original.names())parts.put(name,original.part(name));
        String slide=new String(parts.get("ppt/slides/slide1.xml"),java.nio.charset.StandardCharsets.UTF_8);
        slide=slide.replace("</p:sld>","<p:extLst/></p:sld>");
        parts.put("ppt/slides/slide1.xml",slide.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ByteArrayOutputStream changed=new ByteArrayOutputStream();new OpcPackage(parts).write(changed);
        PptxCodec.ImportResult read=codec.read(new ByteArrayInputStream(changed.toByteArray()));
        assertFalse(read.editable());
        ByteArrayOutputStream saved=new ByteArrayOutputStream();codec.write(read.presentation(),read,saved);
        assertArrayEquals(changed.toByteArray(),saved.toByteArray());
    }
    @Test void presentationOccupiesComponentAndRestoresUi()throws Exception {
        SwingUtilities.invokeAndWait(()->{
            try(PowerPointEditor editor=new PowerPointEditor()){
                var west=((BorderLayout)editor.getLayout()).getLayoutComponent(BorderLayout.WEST);
                editor.insertText("Centered");editor.alignSelected("center");
                PowerPointRibbon ribbon=(PowerPointRibbon)((javax.swing.JPanel)((BorderLayout)editor.getLayout()).getLayoutComponent(BorderLayout.NORTH)).getComponent(0);
                assertEquals("Formato de Texto",ribbon.tabs().getTitleAt(ribbon.tabs().getTabCount()-1));
                PptObject selected=editor.getPresentation().slides().getFirst().objects().getFirst();
                assertEquals((editor.getPresentation().width()-selected.width())/2,selected.x());
                editor.applyTheme("dark");assertEquals(new Color(22,28,40),editor.getPresentation().slides().getFirst().background());
                javax.swing.JComboBox<?> transition=find(editor,"powerpoint.inspector.transition",javax.swing.JComboBox.class);
                assertNotNull(transition);transition.setSelectedItem("Esmaecer");
                assertEquals("fade",editor.getPresentation().slides().getFirst().transition());
                editor.selectObject(null);assertEquals("Exibir",ribbon.tabs().getTitleAt(ribbon.tabs().getTabCount()-1));
                editor.startPresentation();assertTrue(editor.isPresenting());assertFalse(west.isVisible());
                editor.stopPresentation();assertFalse(editor.isPresenting());assertTrue(west.isVisible());
            }
        });
    }
    private static <T extends Component> T find(Component root,String name,Class<T> type){
        if(type.isInstance(root)&&name.equals(root.getName()))return type.cast(root);
        if(root instanceof Container container)for(Component child:container.getComponents()){
            T found=find(child,name,type);if(found!=null)return found;
        }
        return null;
    }
    @Test void fileTasksAndProviderLifecycle(@TempDir Path folder)throws Exception {
        AtomicReference<PowerPointEditor> holder=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->holder.set(new PowerPointEditor()));
        PowerPointEditor editor=holder.get();Path file=folder.resolve("deck.pptx");
        try{
            AtomicInteger calls=new AtomicInteger();AtomicReference<AutoCloseable> registration=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()->{
                editor.insertText("First");
                registration.set(editor.addProvider(new PowerPointCommandProvider(){
                    @Override public String id(){return "test.command";}
                    @Override public Map<String,Action> commands(PowerPointEditor owner){return Map.of("test.run",new AbstractAction(){
                        @Override public void actionPerformed(ActionEvent e){calls.incrementAndGet();}
                    });}
                }));
                assertTrue(editor.invokeCommand("test.run"));
            });
            assertEquals(1,calls.get());
            AtomicReference<dtm.stools.component.panels.editor.powerpoint.api.PowerPointTask<Path>> save=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()->save.set(editor.save(file)));
            assertEquals(file,save.get().completion().toCompletableFuture().get(10,TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(()->editor.setPresentation(Presentation.create()));
            AtomicReference<dtm.stools.component.panels.editor.powerpoint.api.PowerPointTask<PptxCodec.ImportResult>> open=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()->open.set(editor.open(file)));
            assertTrue(open.get().completion().toCompletableFuture().get(10,TimeUnit.SECONDS).editable());
            assertEquals("First",editor.getPresentation().slides().getFirst().objects().getFirst().text());
            SwingUtilities.invokeAndWait(()->{try{registration.get().close();}catch(Exception error){throw new RuntimeException(error);}
                assertFalse(editor.invokeCommand("test.run"));});
        }finally{SwingUtilities.invokeAndWait(editor::close);}
    }

    @Test void repeatedExternalSavesPreserveUnknownParts(@TempDir Path folder)throws Exception {
        PptxCodec codec=new PptxCodec();
        Presentation deck=Presentation.create().withSlide(0,Presentation.create().slides().getFirst()
                .addObject(PptObject.text("Original",40,50,300,90)));
        ByteArrayOutputStream generated=new ByteArrayOutputStream();codec.write(deck,generated);
        OpcPackage own=OpcPackage.read(generated.toByteArray(),OpcPackage.Limits.DEFAULT);
        Map<String,byte[]> parts=new LinkedHashMap<>();
        for(String name:own.names())if(!name.equals("ppt/swingtools.xml"))parts.put(name,own.part(name));
        byte[] unknown="<unknown/>".getBytes(StandardCharsets.UTF_8);
        parts.put("ppt/unknown.xml",unknown);
        Path file=folder.resolve("external.pptx");
        try(OutputStream output=Files.newOutputStream(file)){new OpcPackage(parts).write(output);}

        AtomicReference<PowerPointEditor> holder=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->holder.set(new PowerPointEditor()));
        PowerPointEditor editor=holder.get();
        try {
            AtomicReference<dtm.stools.component.panels.editor.powerpoint.api.PowerPointTask<PptxCodec.ImportResult>> opening=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()->opening.set(editor.open(file)));
            assertTrue(opening.get().completion().toCompletableFuture().get(10,TimeUnit.SECONDS).editable());
            for(String value:List.of("First save","Second save")){
                SwingUtilities.invokeAndWait(()->{
                    PowerPointSession session=editor.getSession();
                    session.edit(doc->{PptSlide slide=doc.slides().getFirst();PptObject object=slide.objects().getFirst();
                        return doc.withSlide(0,slide.replaceObject(object.id(),object.withText(value)));});
                });
                AtomicReference<dtm.stools.component.panels.editor.powerpoint.api.PowerPointTask<Path>> saving=new AtomicReference<>();
                SwingUtilities.invokeAndWait(()->saving.set(editor.save()));
                assertEquals(file,saving.get().completion().toCompletableFuture().get(10,TimeUnit.SECONDS));
                OpcPackage saved=OpcPackage.read(Files.readAllBytes(file),OpcPackage.Limits.DEFAULT);
                assertArrayEquals(unknown,saved.part("ppt/unknown.xml"));
                try(InputStream input=Files.newInputStream(file)){
                    assertEquals(value,codec.read(input).presentation().slides().getFirst().objects().getFirst().text());
                }
            }
        }finally{SwingUtilities.invokeAndWait(editor::close);}
    }
}
