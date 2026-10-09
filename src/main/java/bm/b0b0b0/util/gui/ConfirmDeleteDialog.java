package bm.b0b0b0.util.gui;

import javax.swing.*;
import java.awt.*;

public final class ConfirmDeleteDialog {

    private static final Color BG = Color.decode("#3c3f41");
    private static final Color DANGER = Color.decode("#E57373");

    private ConfirmDeleteDialog() {
    }

    public static boolean show(Window owner, Conf conf, String htmlMessage, int countdownSeconds) {
        final JDialog dialog = new JDialog(owner, conf.getTranslation("dangerDeleteTitle"),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        final boolean[] confirmed = {false};

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));
        content.setBackground(BG);

        JLabel message = new JLabel(htmlMessage);
        message.setForeground(Color.WHITE);
        content.add(message, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel countdownLabel = new JLabel();
        countdownLabel.setForeground(DANGER);
        countdownLabel.setFont(countdownLabel.getFont().deriveFont(Font.BOLD, 13f));
        countdownLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox agree = new JCheckBox(conf.getTranslation("dangerDeleteCheckbox"));
        agree.setForeground(Color.WHITE);
        agree.setOpaque(false);
        agree.setEnabled(false);
        agree.setAlignmentX(Component.LEFT_ALIGNMENT);

        center.add(countdownLabel);
        center.add(Box.createVerticalStrut(8));
        center.add(agree);
        content.add(center, BorderLayout.CENTER);

        JButton yes = new JButton(conf.getTranslation("dangerDeleteYes"));
        yes.setEnabled(false);
        JButton cancel = new JButton(conf.getTranslation("dangerDeleteCancel"));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(yes);
        content.add(buttons, BorderLayout.SOUTH);

        agree.addActionListener(e -> yes.setEnabled(agree.isEnabled() && agree.isSelected()));
        yes.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());

        final int[] remaining = {Math.max(1, countdownSeconds)};
        countdownLabel.setText(String.format(conf.getTranslation("dangerDeleteCountdown"), remaining[0]));

        final Timer timer = new Timer(1000, null);
        timer.addActionListener(e -> {
            remaining[0]--;
            if (remaining[0] > 0) {
                countdownLabel.setText(String.format(conf.getTranslation("dangerDeleteCountdown"), remaining[0]));
            } else {
                timer.stop();
                countdownLabel.setText(conf.getTranslation("dangerDeleteCheckboxPrompt"));
                agree.setEnabled(true);
            }
        });
        timer.setInitialDelay(1000);
        timer.start();

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(520, dialog.getHeight()));
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        timer.stop();

        return confirmed[0];
    }
}
