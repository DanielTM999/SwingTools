package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.panels.editor.powerpoint.PowerPointEditor;
import dtm.stools.component.panels.editor.powerpoint.model.PptAnimation;
import dtm.stools.component.panels.editor.powerpoint.model.PptObject;
import dtm.stools.configs.UiTokens;
import javax.swing.*;

public final class PowerPointEditorExample {
    private PowerPointEditorExample(){}
    public static void main(String[] args){SwingUtilities.invokeLater(()->{
        FlatLightLaf.setup();
        UiTokens.refresh();
        JFrame frame=new JFrame("SwingTools PowerPointEditor");
        JMenuBar menu=new JMenuBar();JMenu appearance=new JMenu("Aparência");
        JMenuItem light=new JMenuItem("FlatLaf claro"),dark=new JMenuItem("FlatLaf escuro");
        light.addActionListener(e->{FlatLightLaf.setup();UiTokens.refresh();SwingUtilities.updateComponentTreeUI(frame);frame.repaint();});
        dark.addActionListener(e->{FlatDarkLaf.setup();UiTokens.refresh();SwingUtilities.updateComponentTreeUI(frame);frame.repaint();});
        appearance.add(light);appearance.add(dark);menu.add(appearance);frame.setJMenuBar(menu);
        PowerPointEditor editor=new PowerPointEditor();
        editor.insertText("Apresentação SwingTools");
        editor.insertShape(PptObject.Kind.RECTANGLE);
        editor.addAnimation(PptAnimation.Effect.FADE_IN);
        editor.addSlide();editor.insertText("Segundo slide");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter(){
            @Override
            public void windowClosed(java.awt.event.WindowEvent e){editor.close();}});
        frame.setContentPane(editor);frame.setSize(1200,750);frame.setLocationRelativeTo(null);frame.setVisible(true);
    });}
}
