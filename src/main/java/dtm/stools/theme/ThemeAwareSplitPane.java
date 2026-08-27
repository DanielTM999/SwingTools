package dtm.stools.theme;

import javax.swing.JSplitPane;
import java.awt.Component;

public class ThemeAwareSplitPane extends JSplitPane {

    private int explicitDividerSize = -1;
    private boolean installingUi;

    public ThemeAwareSplitPane(int orientation) {
        super(orientation);
        setContinuousLayout(true);
        setBorder(null);
    }

    public ThemeAwareSplitPane(int orientation, Component first, Component second) {
        super(orientation, true, first, second);
        setContinuousLayout(true);
        setBorder(null);
    }

    @Override
    public void setDividerSize(int newSize) {
        super.setDividerSize(newSize);
        if (!installingUi) {
            explicitDividerSize = newSize;
        }
    }

    protected void installSplitPaneUi() {
        super.updateUI();
    }

    @Override
    public void updateUI() {
        int dividerLocation = getDividerLocation();
        installingUi = true;
        try {
            installSplitPaneUi();
            setBorder(null);
            if (explicitDividerSize >= 0) {
                super.setDividerSize(explicitDividerSize);
            }
        } finally {
            installingUi = false;
        }

        if (dividerLocation > 0) {
            setDividerLocation(dividerLocation);
        }
    }
}
