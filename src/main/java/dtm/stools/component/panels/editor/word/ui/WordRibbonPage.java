package dtm.stools.component.panels.editor.word.ui;

import com.formdev.flatlaf.util.UIScale;
import dtm.stools.configs.UiTokens;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/** Fits whole groups; controls are never clipped or hidden behind a horizontal scrollbar. */
final class WordRibbonPage extends JPanel {
    private final List<Group> groups = new ArrayList<>();
    private final JButton more = new JButton("Mais ▾");
    WordRibbonPage(JComponent... items) {
        setOpaque(false); setLayout(null);
        for (JComponent item : items) { Group group = (Group)item; groups.add(group); add(group); }
        add(more); more.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            for (Group group : groups) if (!group.isVisible()) {
                JMenuItem item = new JMenuItem(group.name);
                item.addActionListener(event -> group.showPopup(more)); menu.add(item);
            }
            menu.show(more,0,more.getHeight());
        });
    }
    @Override public Dimension getPreferredSize() { return new Dimension(UIScale.scale(600), UIScale.scale(112)); }
    @Override public Dimension getMinimumSize() { return new Dimension(0, UIScale.scale(112)); }
    void applyTheme() { for (Group group : groups) group.applyTheme(); revalidate(); repaint(); }
    void closePopups() { for(Group group:groups)if(group.popup!=null)group.popup.close(); }
    @Override public void doLayout() {
        int available = getWidth(), gap = UIScale.scale(4);
        int[] modes=new int[groups.size()];
        boolean[] visible=new boolean[groups.size()];Arrays.fill(visible,true);
        List<Integer> order=new ArrayList<>();
        for(int i=groups.size()-1;i>=0;i--)order.add(i);
        order.sort(Comparator.comparingInt(i -> groups.get(i).name.equals("Fonte") || groups.get(i).name.equals("Parágrafo") ? 1 : 0));
        for(int mode=1;mode<=2;mode++)for(int i:order){
            if(total(modes,visible,gap)<=available)break;
            modes[i]=mode;
        }
        boolean overflow=total(modes,visible,gap)>available;
        more.setVisible(overflow);
        int reserve=overflow?more.getPreferredSize().width+gap:0;
        for(int i:order){
            if(total(modes,visible,gap)<=available-reserve)break;
            visible[i]=false;
        }
        int x=0;
        for(int i=0;i<groups.size();i++){
            Group group=groups.get(i);group.setVisible(visible[i]);group.mode(modes[i]);
            if(visible[i]){int width=group.width(modes[i]);group.setBounds(x,0,width,getHeight());x+=width+gap;}
        }
        more.setBounds(x,UIScale.scale(30),Math.max(0,Math.min(more.getPreferredSize().width,available-x)),UIScale.scale(32));
    }
    private int total(int[] modes,boolean[] visible,int gap){
        int width=0;for(int i=0;i<groups.size();i++)if(visible[i])width+=groups.get(i).width(modes[i])+gap;
        return width;
    }

    static final class Group extends JPanel {
        private final String name;
        private final JComponent content;
        private final JButton collapsed;
        private final JLabel caption;
        private int mode;
        private final int[] widths = {-1,-1};
        private WordRibbonPopup popup;
        Group(String name, JComponent content) {
            super(new BorderLayout(0,4)); this.name=name;this.content=content;
            setOpaque(false);setBorder(BorderFactory.createEmptyBorder(8,8,4,10));
            caption=new JLabel(name,SwingConstants.CENTER);caption.setFont(UiTokens.fontSmall());caption.setForeground(UiTokens.muted());
            String shortName=switch(name){case "Área de transferência"->"Colar";case "Parágrafo"->"Parágrafo";case "Ilustrações"->"Ilustrar";case "Cabeçalho e rodapé"->"Cabeçalhos";case "Linhas e colunas"->"Estrutura";default->name;};
            collapsed=new JButton(shortName+" ▾",new WordIcon(switch(name){case "Fonte"->"word.bold";case "Parágrafo"->"word.align.LEFT";case "Tabelas"->"word.table";case "Área de transferência"->"word.paste";case "Estilos"->"word.styles";case "Zoom"->"word.zoom.in";default->Objects.requireNonNullElse(representativeCommand(content),"word.palette");},24));
            collapsed.setFont(UiTokens.fontSmall());collapsed.setMargin(new Insets(4,2,4,2));
            collapsed.setVerticalTextPosition(SwingConstants.BOTTOM);collapsed.setHorizontalTextPosition(SwingConstants.CENTER);
            collapsed.setToolTipText(name); collapsed.addActionListener(e -> showPopup(collapsed));
            getAccessibleContext().setAccessibleName(name);add(content);add(caption,BorderLayout.SOUTH);
        }
        int width(int value) {
            if(value==2)return UIScale.scale(88);
            if(widths[value]<0){
                compact(content,value>0);
                widths[value]=Math.max(content.getPreferredSize().width+UIScale.scale(18),caption.getPreferredSize().width+UIScale.scale(18));
                compact(content,mode>0);
            }
            return widths[value];
        }
        private static String representativeCommand(Component component) {
            if(component instanceof AbstractButton button&&button.getIcon() instanceof WordIcon icon)return icon.command();
            if(component instanceof Container container)for(Component child:container.getComponents()){
                String command=representativeCommand(child);if(command!=null)return command;
            }
            return null;
        }
        void applyTheme() {
            if(popup!=null)popup.close();
            caption.setForeground(new Color(UiTokens.muted().getRGB()));
            widths[0]=widths[1]=-1;
            if(content.getParent()!=this && popup==null) SwingUtilities.updateComponentTreeUI(content);
            if(collapsed.getParent()!=this) SwingUtilities.updateComponentTreeUI(collapsed);
        }
        void mode(int value) {
            if(popup!=null && popup.isVisible()){mode=value;return;}
            Component expected=value==2?collapsed:content;
            if(mode==value && expected.getParent()==this)return;
            mode=value;compact(content,value>0);
            remove(content);remove(collapsed);add(expected,BorderLayout.CENTER);caption.setVisible(value!=2);
        }
        @Override public Dimension getPreferredSize() {return new Dimension(width(mode),UIScale.scale(112));}
        void showPopup(Component anchor) {
            if(popup!=null && popup.isVisible()) return;
            compact(content,false);remove(content);
            popup=new WordRibbonPopup(name,content,anchor,()->{
                popup=null;int previous=mode;mode=-1;mode(previous);revalidate();
            });
            popup.show();
        }
        private static void compact(Component component, boolean compact) {
            if(component instanceof AbstractButton button && button.getClientProperty("word.fullText") instanceof String label
                    && !Boolean.TRUE.equals(button.getClientProperty("word.large"))) button.setText(compact ? null : label);
            if(component instanceof Container container) for(Component child:container.getComponents()) compact(child,compact);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);g.setColor(UiTokens.border());g.drawLine(getWidth()-1,10,getWidth()-1,getHeight()-10);
        }
    }
}
