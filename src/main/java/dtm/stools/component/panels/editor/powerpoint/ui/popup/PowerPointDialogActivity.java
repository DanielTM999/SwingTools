package dtm.stools.component.panels.editor.powerpoint.ui.popup;

import dtm.stools.activity.DialogActivity;
import dtm.stools.configs.UiTokens;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.function.Supplier;

/** Modal editor-owned form used for controls larger than a text prompt. */
public final class PowerPointDialogActivity<T> extends DialogActivity {
    private final Component owner;
    private T result;

    public PowerPointDialogActivity(Component owner,String title,JComponent content,Supplier<T> value){
        super(owner instanceof Window window?window:SwingUtilities.getWindowAncestor(owner),title,ModalityType.DOCUMENT_MODAL);
        this.owner=owner;
        applyDrawingOnce();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout(0,12));
        root.setBorder(BorderFactory.createEmptyBorder(16,20,16,20));
        root.add(content,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.TRAILING,8,0));
        JButton cancel=new JButton("Cancelar"),confirm=new JButton("Aplicar");
        confirm.putClientProperty("JButton.buttonType","roundRect");
        cancel.addActionListener(e->dispose());
        confirm.addActionListener(e->{result=value.get();dispose();});
        actions.add(cancel);actions.add(confirm);
        root.add(actions,BorderLayout.SOUTH);
        root.setBackground(UiTokens.background());
        setContentPane(root);
        getRootPane().setDefaultButton(confirm);
        getRootPane().registerKeyboardAction(e->dispose(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    @Override
    protected void onDrawing(){}

    public Optional<T> showResult(){
        if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("Open PowerPoint dialogs on the EDT");
        dispatchDrawing();pack();setLocationRelativeTo(owner);
        try{setVisible(true);return Optional.ofNullable(result);}finally{dispose();}
    }
}
