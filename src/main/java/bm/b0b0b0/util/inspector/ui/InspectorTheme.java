package bm.b0b0b0.util.inspector.ui;

import java.awt.Color;

public final class InspectorTheme {

    public static final Color BACKGROUND = Color.decode("#2b2d30");
    public static final Color CARD = Color.decode("#1e1f22");
    public static final Color FOREGROUND = Color.decode("#e6e6e6");
    public static final Color ACCENT = Color.decode("#09b55d");
    public static final Color BUTTON = Color.decode("#4a4d50");
    public static final Color LINE = Color.decode("#3c3f41");
    public static final Color THREAT = new Color(0xE06666);
    public static final Color WARNING = new Color(0xE6C15E);
    public static final Color CLEAN = new Color(0x8DD07A);

    private InspectorTheme() {
    }

    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
