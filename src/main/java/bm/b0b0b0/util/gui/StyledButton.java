package bm.b0b0b0.util.gui;

import javax.swing.*;
import java.awt.*;

public class StyledButton {

    public enum Style {
        PRIMARY,
        SECONDARY,
        DANGER
    }

    private static final String PRIMARY_STYLE = "background: #10b866;"
            + "foreground: #ffffff;"
            + "hoverBackground: #14c973;"
            + "pressedBackground: #0ea65c;"
            + "borderWidth: 0;";

    private static final String SECONDARY_STYLE = "background: #26292e;"
            + "foreground: #e3e5e8;"
            + "hoverBackground: #2f3238;"
            + "pressedBackground: #373a41;"
            + "borderColor: #33363c;"
            + "hoverBorderColor: #3d4148;";

    private static final String DANGER_STYLE = "background: #b8323d;"
            + "foreground: #ffffff;"
            + "hoverBackground: #cc3a46;"
            + "pressedBackground: #a02a34;"
            + "borderWidth: 0;"
            + "disabledBackground: #3a2427;";

    public static JButton createModernButton(String text, Icon icon) {
        return createModernButton(text, icon, Style.PRIMARY);
    }

    public static JButton createModernButton(String text, Icon icon, Style style) {
        JButton button = icon != null ? new TipButton(text, icon) : new TipButton(text);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (icon != null) {
            button.setIconTextGap(8);
        }
        button.putClientProperty("FlatLaf.style", "arc: 10; margin: 7,16,7,16;"
                + (style == Style.PRIMARY ? "font: bold;" : "")
                + styleFor(style));
        return button;
    }

    public static JButton createModernIconButton(Icon icon, Style style, String tooltip) {
        JButton button = new TipButton(icon);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("FlatLaf.style", "arc: 10; margin: 7,9,7,9;" + styleFor(style));
        return button;
    }

    private static String styleFor(Style style) {
        return switch (style) {
            case PRIMARY -> PRIMARY_STYLE;
            case SECONDARY -> SECONDARY_STYLE;
            case DANGER -> DANGER_STYLE;
        };
    }
}
