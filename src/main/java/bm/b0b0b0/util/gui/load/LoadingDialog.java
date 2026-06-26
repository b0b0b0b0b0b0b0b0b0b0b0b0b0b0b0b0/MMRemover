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

    public LoadingDialog(Frame parent, Conf conf) {
        super(parent, conf.getTranslation("loadingTitle"), false);
        this.parent = parent;
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        initComponents(conf);
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
