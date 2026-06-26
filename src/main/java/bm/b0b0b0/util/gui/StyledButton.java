package bm.b0b0b0.util.gui;

import javax.swing.*;
import java.awt.*;

public class StyledButton {

    public enum Style {
        PRIMARY,
        SECONDARY
    }

    public static JButton createModernButton(String text, Icon icon) {
        return createModernButton(text, icon, Style.PRIMARY);
    }

    public static JButton createModernButton(String text, Icon icon, Style style) {
        JButton button = icon != null ? new JButton(text, icon) : new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(icon != null ? 210 : 172, 36));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("JComponent.focusWidth", 0);
        button.putClientProperty("JComponent.outline", null);
        button.putClientProperty("JComponent.minimumWidth", 168);

        if (icon != null) {
            button.setIconTextGap(8);
            button.setHorizontalTextPosition(SwingConstants.RIGHT);
        }

        String commonStyle = "arc: 6; focusWidth: 0;";

        if (style == Style.PRIMARY) {
            button.putClientProperty("FlatLaf.style", commonStyle
                    + "background: #09b55d;"
                    + "foreground: #FFFFFF;"
                    + "hoverBackground: #0bc266;"
                    + "pressedBackground: #08a052;"
                    + "focusedBackground: #09b55d;"
                    + "disabledBackground: #3c3f41;"
                    + "disabledText: #8a8a8a;");
        } else {
            button.putClientProperty("FlatLaf.style", commonStyle
                    + "background: #4a4d50;"
                    + "foreground: #ebebeb;"
                    + "hoverBackground: #565a5e;"
                    + "pressedBackground: #3e4144;"
                    + "focusedBackground: #4a4d50;"
                    + "disabledBackground: #3c3f41;"
                    + "disabledText: #8a8a8a;");
        }

        return button;
    }

    public static JButton createModernIconButton(Icon icon, Style style, String tooltip) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(36, 36));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("JComponent.focusWidth", 0);
        button.putClientProperty("JComponent.outline", null);
        button.putClientProperty("JComponent.minimumWidth", 36);

        String commonStyle = "arc: 6; focusWidth: 0;";

        if (style == Style.PRIMARY) {
            button.putClientProperty("FlatLaf.style", commonStyle
                    + "background: #09b55d;"
                    + "foreground: #FFFFFF;"
                    + "hoverBackground: #0bc266;"
                    + "pressedBackground: #08a052;"
                    + "focusedBackground: #09b55d;"
                    + "disabledBackground: #3c3f41;"
                    + "disabledText: #8a8a8a;");
        } else {
            button.putClientProperty("FlatLaf.style", commonStyle
                    + "background: #4a4d50;"
                    + "foreground: #ebebeb;"
                    + "hoverBackground: #565a5e;"
                    + "pressedBackground: #3e4144;"
                    + "focusedBackground: #4a4d50;"
                    + "disabledBackground: #3c3f41;"
                    + "disabledText: #8a8a8a;");
        }

        return button;
    }
}
