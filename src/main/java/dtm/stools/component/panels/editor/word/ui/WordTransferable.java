package dtm.stools.component.panels.editor.word.ui;

import dtm.stools.component.panels.editor.word.io.WordTextExporter;
import dtm.stools.component.panels.editor.word.model.WordDocument;
import java.awt.Image;
import java.awt.datatransfer.*;
import java.util.ArrayList;
import java.util.List;

public final class WordTransferable implements Transferable {
    public static final DataFlavor DOCUMENT=new DataFlavor(WordDocument.class,"SwingTools Word fragment");
    public static final DataFlavor HTML=new DataFlavor("text/html;class=java.lang.String","HTML");
    private final WordDocument fragment;
    private final String plain;
    private final Image image;
    public WordTransferable(WordDocument fragment){this(fragment,null,null);}
    public WordTransferable(WordDocument fragment,String plain,Image image){this.fragment=java.util.Objects.requireNonNull(fragment);this.plain=plain;this.image=image;}
    @Override public DataFlavor[] getTransferDataFlavors(){
        List<DataFlavor> flavors=new ArrayList<>(List.of(DOCUMENT,HTML,DataFlavor.stringFlavor));if(image!=null)flavors.add(DataFlavor.imageFlavor);
        return flavors.toArray(DataFlavor[]::new);
    }
    @Override public boolean isDataFlavorSupported(DataFlavor flavor){return flavor.equals(DOCUMENT)||flavor.equals(HTML)||flavor.equals(DataFlavor.stringFlavor)||image!=null&&flavor.equals(DataFlavor.imageFlavor);}
    @Override public Object getTransferData(DataFlavor flavor)throws UnsupportedFlavorException{
        if(flavor.equals(DOCUMENT))return fragment;
        if(flavor.equals(HTML))return WordTextExporter.html(fragment);
        if(flavor.equals(DataFlavor.stringFlavor))return plain!=null?plain:WordTextExporter.text(fragment);
        if(image!=null&&flavor.equals(DataFlavor.imageFlavor))return image;
        throw new UnsupportedFlavorException(flavor);
    }
}
