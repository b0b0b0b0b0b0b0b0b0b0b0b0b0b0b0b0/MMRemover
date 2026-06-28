package bm.b0b0b0.util.gui;

import bm.b0b0b0.util.AppLinks;
import bm.b0b0b0.util.update.AppVersion;
import bm.b0b0b0.util.update.UpdateChecker;
import bm.b0b0b0.util.update.UpdateInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;

public final class UpdateDialog extends JDialog {
    private static final Color COLOR_BG = Color.decode("#3c3f41");
    private static final Color COLOR_TEXT = new Color(0xEBEBEB);
    private static final Color COLOR_MUTED = new Color(0xA8A8A8);

    private UpdateDialog(Window parent, Conf conf, UpdateInfo update) {
        super(parent, conf.getTranslation("updateAvailableTitle"), ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        buildUi(conf, update);
        pack();
        setLocationRelativeTo(parent);
    }

    public static void show(Component parent, Conf conf, UpdateInfo update) {
        Window window = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        UpdateDialog dialog = new UpdateDialog(window, conf, update);
        dialog.setVisible(true);
    }

    private void buildUi(Conf conf, UpdateInfo update) {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(18, 20, 18, 20));
        setContentPane(root);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(
                    getClass().getResource("/icons/logo_tr.png")));
            Image scaled = icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            header.add(new JLabel(new ImageIcon(scaled)), BorderLayout.WEST);
        } catch (Exception ignored) {
        }

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(conf.getTranslation("updateAvailableTitle"));
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(COLOR_TEXT);
        titles.add(title);

        JLabel subtitle = new JLabel(String.format(
                conf.getTranslation("updateAvailableSubtitle"),
                AppVersion.CURRENT,
                update.version()
        ));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(COLOR_MUTED);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        titles.add(subtitle);
        header.add(titles, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        if (update.message() != null && !update.message().isBlank()) {
            JTextArea body = new JTextArea(update.message());
            body.setEditable(false);
            body.setLineWrap(true);
            body.setWrapStyleWord(true);
            body.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            body.setForeground(COLOR_TEXT);
            body.setBackground(new Color(0x2B2D30));
            body.setBorder(new EmptyBorder(12, 12, 12, 12));
            body.setCaretPosition(0);

            JScrollPane scroll = new JScrollPane(body);
            scroll.setBorder(BorderFactory.createLineBorder(new Color(0x258e66)));
            scroll.setPreferredSize(new Dimension(380, 120));
            root.add(scroll, BorderLayout.CENTER);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        JButton laterButton = StyledButton.createModernButton(
                conf.getTranslation("updateLaterButton"),
                null,
                StyledButton.Style.SECONDARY
        );
        laterButton.addActionListener(e -> dispose());
        buttons.add(laterButton);

        String downloadUrl = update.downloadUrl();
        if (downloadUrl == null || downloadUrl.isBlank()) {
            downloadUrl = AppLinks.PROGRAM_PAGE;
        }
        JButton downloadButton = StyledButton.createModernButton(
                conf.getTranslation("updateDownloadButton"),
                null,
                StyledButton.Style.PRIMARY
        );
        String finalDownloadUrl = downloadUrl;
        downloadButton.addActionListener(e -> {
            if (!UpdateChecker.openInBrowser(finalDownloadUrl)) {
                JOptionPane.showMessageDialog(
                        this,
                        conf.getTranslation("updateOpenLinkFailed"),
                        conf.getTranslation("updateAvailableTitle"),
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });
        buttons.add(downloadButton);

        root.add(buttons, BorderLayout.SOUTH);
    }
}
