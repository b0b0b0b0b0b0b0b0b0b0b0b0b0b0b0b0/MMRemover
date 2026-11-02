package bm.b0b0b0.util.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Кнопка, у которой подсказка появляется по центру под ней (или над ней,
 * если снизу не хватает места в окне), а не у курсора.
 */
public class TipButton extends JButton {
    private static final int TIP_GAP = 6;

    public TipButton() {
    }

    public TipButton(Icon icon) {
        super(icon);
    }

    public TipButton(String text) {
        super(text);
    }

    public TipButton(String text, Icon icon) {
        super(text, icon);
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        String text = getToolTipText(event);
        if (text == null) {
            return null;
        }
        JToolTip tip = createToolTip();
        tip.setTipText(text);
        Dimension size = tip.getPreferredSize();

        int x = (getWidth() - size.width) / 2;
        int y = getHeight() + TIP_GAP;

        JRootPane root = getRootPane();
        if (root != null) {
            Point inRoot = SwingUtilities.convertPoint(this, 0, 0, root);
            if (inRoot.y + y + size.height > root.getHeight()) {
                y = -size.height - TIP_GAP;
            }
            int minX = -inRoot.x + TIP_GAP;
            int maxX = root.getWidth() - inRoot.x - size.width - TIP_GAP;
            x = Math.max(minX, Math.min(x, maxX));
        }
        return new Point(x, y);
    }
}
