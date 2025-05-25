package bm.b0b0b0.util.gui.load;

import bm.b0b0b0.util.gui.Conf;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class LoadingDialog extends JDialog {

    public LoadingDialog(Frame parent, Conf conf) {
        super(parent, conf.getTranslation("loadingTitle"), true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        initComponents(conf);
        setLocationRelativeTo(parent);
    }
    private void initComponents(Conf conf) {
        setLayout(new BorderLayout());
        ImageIcon loadingIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/icons/armadillo.gif"))
        );
        JLabel gifLabel = new JLabel(loadingIcon);
        gifLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gifLabel.setVerticalAlignment(SwingConstants.CENTER);
        getContentPane().setBackground(new Color(0,0,0,0));
        add(gifLabel, BorderLayout.CENTER);
        pack();

    }
}
