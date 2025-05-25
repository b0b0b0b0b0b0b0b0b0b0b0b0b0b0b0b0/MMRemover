package bm.b0b0b0.util.gui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class StyledButton {

    public static JButton createModernButton(String text, String iconPath) {
        JButton button = new JButton(text);
        button.setForeground(Color.decode("#303030"));
        button.setFont(new Font("Roboto", Font.BOLD, 13));
        button.setPreferredSize(new Dimension(180, 30));

        if (iconPath != null) button.setIcon(new ImageIcon(iconPath));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);

        Timer timer = new Timer(50, e -> {
            float hue = (System.currentTimeMillis() % 10000) / 10000f;
            button.setBackground(Color.getHSBColor(hue, 0.6f, 0.5f));
            button.repaint();
        });
        timer.start();

        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, button.getBackground(), button.getWidth(), button.getHeight(), button.getBackground().brighter());
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), 15, 15);

                g2d.setColor(button.getForeground());
                g2d.drawRoundRect(0, 0, button.getWidth() - 1, button.getHeight() - 1, 15, 15);

                super.paint(g, c);
            }

            @Override
            protected void paintButtonPressed(Graphics g, AbstractButton b) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setPaint(new GradientPaint(0, 0, new Color(0x258f66), b.getWidth(), b.getHeight(), new Color(0x33af67)));
                g2d.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), 15, 15);
            }
        });

        int cornerRadius = 15;
        button.setBorder(new RoundedBorder(cornerRadius));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setOpaque(true);
                button.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setOpaque(false);
                button.repaint();
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBorder(new RoundedBorder(cornerRadius - 5));
                button.setFont(new Font("Roboto", Font.BOLD, 12));
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBorder(new RoundedBorder(cornerRadius));
                button.setFont(new Font("Roboto", Font.BOLD, 14));
            }
        });

        return button;
    }

    public static class RoundedBorder implements Border {
        private final int radius;

        public RoundedBorder(int radius) {
            this.radius = radius;
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(radius + 1, radius + 1, radius + 2, radius);
        }

        public boolean isBorderOpaque() {
            return true;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
    }
}
