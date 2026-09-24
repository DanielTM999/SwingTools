package dtm.stools.component.panels.editor.word.ui;

import dtm.stools.configs.UiTokens;
import com.formdev.flatlaf.util.UIScale;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

public final class WordNavigationPanel extends JPanel {
    public WordNavigationPanel(JList<String> outline, Runnable close) {
        super(new BorderLayout());
        setPreferredSize(UIScale.scale(new Dimension(240,0)));setName("word.navigation.panel");
        add(header("Navegação",close),BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(outline);scroll.setBorder(BorderFactory.createEmptyBorder(4,8,8,8));
        outline.setCellRenderer((list,value,index,selected,focus)->{
            JLabel label=new JLabel(value);label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(7,8,7,8));
            label.setBackground(selected?UiTokens.accent():UiTokens.surface());
            label.setForeground(selected?UiTokens.onColor(UiTokens.accent()):UiTokens.foreground());return label;
        });
        JPanel center=new JPanel(new CardLayout());
        JLabel empty=new JLabel("Nenhum título no documento",SwingConstants.CENTER);empty.setForeground(UiTokens.muted());
        center.add(scroll,"outline");center.add(empty,"empty");add(center);
        Runnable refresh=()->((CardLayout)center.getLayout()).show(center,outline.getModel().getSize()==0?"empty":"outline");
        outline.getModel().addListDataListener(new ListDataListener(){
            public void intervalAdded(ListDataEvent e){refresh.run();}public void intervalRemoved(ListDataEvent e){refresh.run();}public void contentsChanged(ListDataEvent e){refresh.run();}
        });refresh.run();
    }
    public static JComponent header(String text,Runnable close) {
        JPanel header=new JPanel(new BorderLayout(8,0));header.setOpaque(false);header.setBorder(BorderFactory.createEmptyBorder(10,14,8,10));
        JLabel title=new JLabel(text);title.setFont(UiTokens.fontBold());header.add(title);
        JButton button=new JButton(new WordIcon("close",16));button.setName("word.panel.close");button.setToolTipText("Fechar "+text.toLowerCase());
        button.getAccessibleContext().setAccessibleName(button.getToolTipText());button.putClientProperty("JButton.buttonType","toolBarButton");
        button.addActionListener(e->close.run());header.add(button,BorderLayout.EAST);return header;
    }
}
