package dtm.stools.component.panels.tab;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class TabStyle {

    private String closeButtonText = "x";
    private String closeButtonTooltip = "Fechar";

    private Color tabTitleForeground;
    private Color selectedTabTitleForeground;

    private Color tabAreaBackground;

    private Color tabAreaTopLineColor;
    private Color tabAreaBottomLineColor;
    private Color tabAreaLeftLineColor;
    private Color tabAreaRightLineColor;

    private Color tabHeaderBottomLineSeparatorColor;

    private Color tabHeaderBackground;
    private Color selectedTabHeaderBackground;

    private Color tabHeaderBorderColor;
    private Color selectedTabHeaderBorderColor;

    private Color tabBadgeBackground;
    private Color tabBadgeForeground;

    private Font tabTitleFont;
    private Font selectedTabTitleFont;
    private Font tabBadgeFont;

    private Insets tabAreaInsets;
    private Insets tabInsets;
    private Insets selectedTabPadInsets;
    private Insets contentBorderInsets;

    private Insets tabHeaderPadding;
    private Insets tabHeaderBackgroundInsets;

    private int tabHeaderGap;
    private int tabHeaderArc;
    private int tabBadgeArc;
    private int minTabHeight;
    private int tabAreaBottomLineGap;

    private boolean paintTabAreaBackground;

    private boolean paintTabAreaTopLine;
    private boolean paintTabAreaBottomLine;
    private boolean paintTabAreaLeftLine;
    private boolean paintTabAreaRightLine;

    private boolean paintContentBorder;
    private boolean paintFocusIndicator;
    private boolean paintOnlySelectedTabBackground;
    private boolean paintOnlySelectedTabBorder;
    private boolean selectedTabCoversBottomLine;

    private Icon defaultTabIcon;
    private Icon defaultSelectedTabIcon;

    private String closeButtonIconResource;
    private String pinnedButtonIconResource;
    private String unpinnedButtonIconResource;

    private int closeButtonIconSize;
    private int pinnedButtonIconSize;

    private float tabHeaderBorderWidth;
    private float selectedTabHeaderBorderWidth;

    private Color closeButtonIconColor;
    private Color pinnedButtonIconColor;

    public TabStyle() {
        Color panel = color("Panel.background", new Color(43, 45, 48));
        Color foreground = color("Label.foreground", new Color(220, 221, 222));
        Color border = color("Component.borderColor", panel.darker());

        Color selected = panel.darker();
        Color selectedBorder = panel.brighter();
        Color badge = panel.brighter();

        this.tabTitleForeground = foreground.darker();
        this.selectedTabTitleForeground = foreground;

        this.tabAreaBackground = null;

        this.tabHeaderBorderWidth = 1f;
        this.selectedTabHeaderBorderWidth = 1.0f;

        this.tabAreaTopLineColor = null;
        this.tabAreaBottomLineColor = null;
        this.tabAreaLeftLineColor = null;
        this.tabAreaRightLineColor = null;

        this.tabHeaderBottomLineSeparatorColor = border;

        this.tabHeaderBackground = null;
        this.selectedTabHeaderBackground = selected;

        this.tabHeaderBorderColor = null;
        this.selectedTabHeaderBorderColor = selectedBorder;

        this.tabBadgeBackground = badge;
        this.tabBadgeForeground = foreground;

        Font labelFont = UIManager.getFont("Label.font");
        this.tabTitleFont = labelFont;
        this.selectedTabTitleFont = labelFont;
        this.tabBadgeFont = labelFont == null
                ? null
                : labelFont.deriveFont(Font.BOLD, Math.max(9f, labelFont.getSize2D() - 2f));

        this.tabAreaInsets = new Insets(4, 8, 4, 8);

        this.tabInsets = new Insets(0, 0, 0, 0);
        this.selectedTabPadInsets = new Insets(0, 0, 0, 0);
        this.contentBorderInsets = new Insets(0, 0, 0, 0);

        this.tabHeaderPadding = new Insets(5, 10, 5, 10);
        this.tabHeaderBackgroundInsets = new Insets(3, 2, 3, 2);

        this.tabHeaderGap = 3;
        this.tabHeaderArc = 10;
        this.tabBadgeArc = 8;

        this.minTabHeight = 34;
        this.tabAreaBottomLineGap = 0;

        this.paintTabAreaBackground = false;

        this.paintTabAreaTopLine = false;
        this.paintTabAreaBottomLine = true;
        this.paintTabAreaLeftLine = false;
        this.paintTabAreaRightLine = false;

        this.paintContentBorder = false;
        this.paintFocusIndicator = false;
        this.paintOnlySelectedTabBackground = true;
        this.paintOnlySelectedTabBorder = true;
        this.selectedTabCoversBottomLine = false;

        this.closeButtonIconResource = "/drawables/close.png";
        this.pinnedButtonIconResource = "/drawables/tabHeader/pin-filled.png";
        this.unpinnedButtonIconResource = "/drawables/tabHeader/pin.png";

        this.closeButtonIconSize = 8;
        this.pinnedButtonIconSize = 12;

        this.closeButtonIconColor = foreground.darker();
        this.pinnedButtonIconColor = foreground.darker();
    }

    private static Color color(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color == null ? fallback : color;
    }

    public void copyTo(TabStyle target) {
        target.closeButtonText = closeButtonText;
        target.closeButtonTooltip = closeButtonTooltip;

        target.tabTitleForeground = tabTitleForeground;
        target.selectedTabTitleForeground = selectedTabTitleForeground;

        target.tabAreaBackground = tabAreaBackground;

        target.tabAreaTopLineColor = tabAreaTopLineColor;
        target.tabAreaBottomLineColor = tabAreaBottomLineColor;
        target.tabAreaLeftLineColor = tabAreaLeftLineColor;
        target.tabAreaRightLineColor = tabAreaRightLineColor;

        target.tabHeaderBackground = tabHeaderBackground;
        target.selectedTabHeaderBackground = selectedTabHeaderBackground;

        target.tabHeaderBorderColor = tabHeaderBorderColor;
        target.selectedTabHeaderBorderColor = selectedTabHeaderBorderColor;

        target.tabHeaderBottomLineSeparatorColor = tabHeaderBottomLineSeparatorColor;

        target.tabBadgeBackground = tabBadgeBackground;
        target.tabBadgeForeground = tabBadgeForeground;

        target.tabTitleFont = tabTitleFont;
        target.selectedTabTitleFont = selectedTabTitleFont;
        target.tabBadgeFont = tabBadgeFont;

        target.tabAreaInsets = tabAreaInsets;
        target.tabInsets = tabInsets;
        target.selectedTabPadInsets = selectedTabPadInsets;
        target.contentBorderInsets = contentBorderInsets;

        target.tabHeaderPadding = tabHeaderPadding;
        target.tabHeaderBackgroundInsets = tabHeaderBackgroundInsets;

        target.tabHeaderGap = tabHeaderGap;
        target.tabHeaderArc = tabHeaderArc;
        target.tabBadgeArc = tabBadgeArc;
        target.minTabHeight = minTabHeight;
        target.tabAreaBottomLineGap = tabAreaBottomLineGap;

        target.paintTabAreaBackground = paintTabAreaBackground;

        target.paintTabAreaTopLine = paintTabAreaTopLine;
        target.paintTabAreaBottomLine = paintTabAreaBottomLine;
        target.paintTabAreaLeftLine = paintTabAreaLeftLine;
        target.paintTabAreaRightLine = paintTabAreaRightLine;

        target.paintContentBorder = paintContentBorder;
        target.paintFocusIndicator = paintFocusIndicator;
        target.paintOnlySelectedTabBackground = paintOnlySelectedTabBackground;
        target.paintOnlySelectedTabBorder = paintOnlySelectedTabBorder;
        target.selectedTabCoversBottomLine = selectedTabCoversBottomLine;

        target.defaultTabIcon = defaultTabIcon;
        target.defaultSelectedTabIcon = defaultSelectedTabIcon;

        target.closeButtonIconResource = closeButtonIconResource;
        target.pinnedButtonIconResource = pinnedButtonIconResource;
        target.unpinnedButtonIconResource = unpinnedButtonIconResource;

        target.closeButtonIconSize = closeButtonIconSize;
        target.pinnedButtonIconSize = pinnedButtonIconSize;

        target.closeButtonIconColor = closeButtonIconColor;
        target.pinnedButtonIconColor = pinnedButtonIconColor;
    }
}