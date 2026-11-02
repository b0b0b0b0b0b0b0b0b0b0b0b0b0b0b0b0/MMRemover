package bm.b0b0b0.util.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public final class LanguageSwitcher {
    private static final int FLAG_SIZE = 16;
    private static final int BUTTON_SIZE = 34;

    private LanguageSwitcher() {
    }

    private record LangItem(String code, String label, String iconPath) {
    }

    public static JButton create(Conf conf, Runnable onLanguageChanged) {
        LangItem[] items = {
                new LangItem("ru", "Русский", "/icons/ru.png"),
                new LangItem("en", "English", "/icons/en.png")
        };

        LangItem current = findItem(items, conf.getLang());
        JButton button = new TipButton(loadFlagIcon(current.iconPath()));
        button.setToolTipText(current.label() + " — " + conf.getTranslation("languageSwitcherTooltip"));
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.putClientProperty("FlatLaf.style", "arc: 8");

        JPopupMenu menu = new JPopupMenu();

        for (LangItem item : items) {
            JMenuItem menuItem = new JMenuItem(item.label(), loadFlagIcon(item.iconPath()));
            menuItem.setIconTextGap(10);
            menuItem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            menuItem.addActionListener(e -> {
                if (!item.code().equals(conf.getLang())) {
                    conf.setLang(item.code());
                    onLanguageChanged.run();
                }
            });
            menu.add(menuItem);
        }

        button.addActionListener(e -> {
            menu.pack();
            int x = button.getWidth() - menu.getPreferredSize().width;
            menu.show(button, x, button.getHeight());
        });

        return button;
    }

    private static LangItem findItem(LangItem[] items, String code) {
        for (LangItem item : items) {
            if (item.code().equals(code)) {
                return item;
            }
        }
        return items[0];
    }

    private static Icon loadFlagIcon(String iconPath) {
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(LanguageSwitcher.class.getResource(iconPath)));
            Image scaled = icon.getImage().getScaledInstance(FLAG_SIZE, FLAG_SIZE, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }
}
