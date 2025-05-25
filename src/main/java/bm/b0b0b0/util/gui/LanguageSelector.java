package bm.b0b0b0.util.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class LanguageSelector {
    private static final String CONFIG_FILE = "config.yml";

    public LanguageSelector(Runnable onLanguageSelected) {
        JFrame frame = new JFrame("Выбор языка / Select Language");
        setAppIcon(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.decode("#1e1e1e"));
        frame.add(mainPanel);

        JLabel label = new JLabel("Выберите язык / Select Language:");
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(label, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.decode("#1e1e1e"));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        JButton russianButton = createLanguageButton("Русский", "/icons/ru.png");
        JButton englishButton = createLanguageButton("English", "/icons/en.png");

        buttonPanel.add(russianButton);
        buttonPanel.add(englishButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        russianButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateConfigLanguage("ru");
                frame.dispose();
                onLanguageSelected.run();
            }
        });

        englishButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateConfigLanguage("en");
                frame.dispose();
                onLanguageSelected.run();
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JButton createLanguageButton(String text, String iconPath) {
        JButton button = new JButton(text);
        try {
            ImageIcon icon = resizeIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource(iconPath))));
            button.setIcon(icon);
        } catch (Exception e) {
            e.printStackTrace();
        }
        button.setForeground(Color.WHITE);
        button.setBackground(Color.decode("#3a3a3a"));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#3a3a3a")),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        button.setFocusPainted(false);
        button.setFont(new Font("Courier New", Font.PLAIN, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.decode("#4a4a4a"));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.decode("#3a3a3a"));
            }
        });

        return button;
    }

    private ImageIcon resizeIcon(ImageIcon icon) {
        Image img = icon.getImage();
        int newHeight = (icon.getIconHeight() * 30) / icon.getIconWidth();
        Image resizedImage = img.getScaledInstance(30, newHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(resizedImage);
    }

    private void updateConfigLanguage(String lang) {
        try {
            Conf conf = new Conf();
            conf.setLang(lang);
            conf.saveConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setAppIcon(JFrame frame) {
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/bm.png")));
            Image img = icon.getImage();
            frame.setIconImage(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showLanguageSelector(Runnable onLanguageSelected) {
        Path configPath = Paths.get(CONFIG_FILE);
        if (!Files.exists(configPath)) {
            new LanguageSelector(onLanguageSelected);
        } else {
            onLanguageSelected.run();
        }
    }
}
