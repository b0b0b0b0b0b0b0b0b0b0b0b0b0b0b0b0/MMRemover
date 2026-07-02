package bm.b0b0b0.util.gui.load;

import bm.b0b0b0.util.gui.Conf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Objects;

public class LoadingDialog extends JDialog {

    private final Frame parent;
    private final ComponentAdapter followParentListener;
    private final JLabel statusLabel;

    public LoadingDialog(Frame parent, Conf conf) {
        this(parent, conf, "loadingTitle");
    }

    public LoadingDialog(Frame parent, Conf conf, String titleKey) {
        super(parent, conf.getTranslation(titleKey), false);
        this.parent = parent;
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        statusLabel = initComponents(conf);
        setLocationRelativeTo(parent);

        followParentListener = new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                reposition();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                reposition();
            }
        };
        parent.addComponentListener(followParentListener);
    }

    public void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text == null ? "" : text);
        }
    }

    private void reposition() {
        if (isDisplayable()) {
            setLocationRelativeTo(parent);
        }
    }

    @Override
    public void dispose() {
        parent.removeComponentListener(followParentListener);
        super.dispose();
    }

    private JLabel initComponents(Conf conf) {
        setLayout(new BorderLayout(0, 8));
        ImageIcon loadingIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/icons/armadillo.gif"))
        );
        JLabel gifLabel = new JLabel(loadingIcon);
        gifLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gifLabel.setVerticalAlignment(SwingConstants.CENTER);

        JLabel status = new JLabel("", SwingConstants.CENTER);
        status.setForeground(Color.WHITE);
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        content.add(gifLabel, BorderLayout.CENTER);
        content.add(status, BorderLayout.SOUTH);

        getContentPane().setBackground(new Color(0, 0, 0, 0));
        add(content, BorderLayout.CENTER);
        pack();
        return status;
    }
}
