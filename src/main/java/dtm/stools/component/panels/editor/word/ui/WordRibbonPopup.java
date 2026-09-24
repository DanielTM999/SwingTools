package dtm.stools.component.panels.editor.word.ui;

import dtm.stools.configs.UiTokens;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** A component popup whose lifetime is independent of nested Swing menu selection paths. */
final class WordRibbonPopup implements AutoCloseable {
    private final JPanel holder = new JPanel(new BorderLayout(0,8));
    private final JComponent content;
    private final Component anchor;
    private final Runnable closed;
    private final AWTEventListener events = this::event;
    private final HierarchyListener hierarchy = e -> { if (!anchorShowing()) close(); };
    private Popup popup;

    WordRibbonPopup(String title,JComponent content,Component anchor,Runnable closed) {
        this.content=content;this.anchor=anchor;this.closed=closed;
        holder.setName("word.ribbon.popup");holder.setFocusCycleRoot(true);
        holder.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTokens.border()),BorderFactory.createEmptyBorder(12,12,12,12)));
        JLabel heading=new JLabel(title);heading.setFont(UiTokens.fontBold());holder.add(heading,BorderLayout.NORTH);holder.add(content);
    }
    void show() {
        Point point=anchor.getLocationOnScreen();point.y+=anchor.getHeight();
        Rectangle screen=new Rectangle(anchor.getGraphicsConfiguration().getBounds());
        Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(anchor.getGraphicsConfiguration());
        screen.x+=insets.left;screen.y+=insets.top;screen.width-=insets.left+insets.right;screen.height-=insets.top+insets.bottom;
        Dimension size=holder.getPreferredSize();
        if(size.width>screen.width || size.height>screen.height){
            holder.remove(content);JScrollPane scroll=new JScrollPane(content);scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setPreferredSize(new Dimension(Math.min(size.width-26,screen.width-26),Math.min(size.height-50,screen.height-50)));holder.add(scroll);size=holder.getPreferredSize();
        }
        point.x=Math.max(screen.x,Math.min(point.x,screen.x+screen.width-size.width));
        point.y=Math.max(screen.y,Math.min(point.y,screen.y+screen.height-size.height));
        popup=PopupFactory.getSharedInstance().getPopup(anchor,holder,point.x,point.y);
        popup.show();
        Toolkit.getDefaultToolkit().addAWTEventListener(events,AWTEvent.MOUSE_EVENT_MASK|AWTEvent.KEY_EVENT_MASK|AWTEvent.WINDOW_EVENT_MASK);
        anchor.addHierarchyListener(hierarchy);
        holder.transferFocus();
    }
    boolean isVisible(){return popup!=null;}
    private boolean anchorShowing(){return anchor.isShowing();}
    private void event(AWTEvent event){
        if(event instanceof MouseEvent mouse && mouse.getID()==MouseEvent.MOUSE_PRESSED && !inside(mouse.getComponent()))close();
        else if(event instanceof KeyEvent key && key.getID()==KeyEvent.KEY_PRESSED && key.getKeyCode()==KeyEvent.VK_ESCAPE
                && MenuSelectionManager.defaultManager().getSelectedPath().length==0){close();key.consume();}
        else if(event instanceof WindowEvent window && (window.getID()==WindowEvent.WINDOW_CLOSING || window.getID()==WindowEvent.WINDOW_CLOSED)
                && window.getWindow()==SwingUtilities.getWindowAncestor(anchor))close();
    }
    private boolean inside(Component component){
        if(component==anchor)return true;
        while(component!=null){
            if(component==holder)return true;
            if(component instanceof JPopupMenu menu)component=menu.getInvoker();
            else component=component.getParent();
        }
        return false;
    }
    @Override public void close(){
        if(popup==null)return;
        Toolkit.getDefaultToolkit().removeAWTEventListener(events);anchor.removeHierarchyListener(hierarchy);
        Popup old=popup;popup=null;old.hide();
        if(content.getParent()!=null)content.getParent().remove(content);
        closed.run();if(anchor.isShowing())anchor.requestFocusInWindow();
    }
}
