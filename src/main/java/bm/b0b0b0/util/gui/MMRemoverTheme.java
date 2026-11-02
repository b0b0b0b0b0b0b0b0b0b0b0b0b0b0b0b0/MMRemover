package bm.b0b0b0.util.gui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Тёмная тема приложения. Цвета и скругления лежат в MMRemoverTheme.properties
 * (FlatLaf подхватывает его автоматически по имени класса).
 */
public class MMRemoverTheme extends FlatDarkLaf {
    public static final String NAME = "MMRemover Dark";

    public static boolean setup() {
        boolean installed = setup(new MMRemoverTheme());
        ToolTipManager tips = ToolTipManager.sharedInstance();
        tips.setInitialDelay(250);
        tips.setReshowDelay(0);
        tips.setDismissDelay(15000);
        return installed;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public static Color cardBackground() {
        return UIManager.getColor("MMRemover.cardBackground");
    }

    public static Color cardBorderColor() {
        return UIManager.getColor("MMRemover.cardBorderColor");
    }

    public static Color hoverBackground() {
        return UIManager.getColor("MMRemover.hoverBackground");
    }

    public static Color selectedBackground() {
        return UIManager.getColor("MMRemover.selectedBackground");
    }

    public static Color mutedForeground() {
        return UIManager.getColor("MMRemover.mutedForeground");
    }

    public static Color accentColor() {
        return UIManager.getColor("MMRemover.accentColor");
    }

    public static Color dangerColor() {
        return UIManager.getColor("MMRemover.dangerColor");
    }

    public static Color consoleBackground() {
        return UIManager.getColor("MMRemover.consoleBackground");
    }

    public static Color consoleForeground() {
        return UIManager.getColor("MMRemover.consoleForeground");
    }

    public static Font monospaceFont(float size) {
        for (String name : new String[]{"Cascadia Mono", "JetBrains Mono", "Consolas"}) {
            Font font = new Font(name, Font.PLAIN, 12);
            if (name.equals(font.getFamily())) {
                return font.deriveFont(size);
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, 12).deriveFont(size);
    }
}
