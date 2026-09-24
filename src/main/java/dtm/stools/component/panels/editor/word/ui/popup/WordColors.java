package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.WordEditor;
import dtm.stools.component.panels.editor.word.provider.WordDialogRequest;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class WordColors {
    private WordColors() {}
    public static void show(Component owner, String title, Color initial, Consumer<Color> apply, Runnable clear) {
        JPopupMenu popup=new JPopupMenu();
        JPanel palette=new JPanel(new GridLayout(3,6,4,4));palette.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        int[] colors={0x111111,0x595959,0xFFFFFF,0x203864,0x2F5496,0x4472C4,0xC00000,0xED7D31,0xFFC000,0xFFFF00,0x70AD47,0x008080,0x7030A0,0xD5E3F5,0xFBE2D5,0xFFF2CC,0xE2EFD9,0xE4DFEC};
        for(int rgb:colors) {
            JButton swatch=new JButton();swatch.setPreferredSize(new Dimension(28,28));swatch.setBackground(new Color(rgb));
            swatch.setOpaque(true);swatch.setToolTipText(String.format("#%06X",rgb));swatch.getAccessibleContext().setAccessibleName(swatch.getToolTipText());
            swatch.addActionListener(e->{popup.setVisible(false);apply.accept(new Color(rgb));});palette.add(swatch);
        }
        popup.add(palette);
        if(clear!=null){JMenuItem none=new JMenuItem("Sem cor");none.addActionListener(e->clear.run());popup.add(none);}
        JMenuItem more=new JMenuItem("Mais cores…");more.addActionListener(e->{
            JColorChooser chooser=new JColorChooser(initial);
            WordDialogRequest<Color> request=new WordDialogRequest<>(owner,"word.color.dialog",title,"",chooser,chooser::getColor,value->{},"Aplicar",false,false);
            WordEditor editor=findEditor(owner);
            if(editor!=null) editor.getDialogProvider().show(request).ifPresent(apply);
            else try(DefaultDialogProvider provider=new DefaultDialogProvider()){provider.show(request).ifPresent(apply);}
        });popup.add(more);
        Component focus=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        Component anchor=focus instanceof AbstractButton && SwingUtilities.isDescendingFrom(focus,owner)?focus:owner;
        popup.show(anchor,anchor instanceof AbstractButton?0:Math.min(24,anchor.getWidth()),anchor instanceof AbstractButton?anchor.getHeight():Math.min(24,anchor.getHeight()));
    }
    private static WordEditor findEditor(Component component) {
        while(component!=null){
            if(component instanceof WordEditor editor)return editor;
            if(component instanceof WordDialogActivity<?> dialog)component=dialog.sourceOwner();
            else component=component.getParent();
        }
        return null;
    }
}
