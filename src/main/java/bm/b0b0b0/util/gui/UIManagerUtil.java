package bm.b0b0b0.util.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;

public class UIManagerUtil {
    private static final Color PANEL_BORDER_COLOR = new Color(0x258e66);
    private static final int PANEL_BORDER_RADIUS = 10;
    private static final String REFRESH_ICON_PATH = "/icons/reload.png";
    private static final String BROOM_ICON_PATH = "/icons/broom.png";
    private static final String ERASER_ICON_PATH = "/icons/eraser.png";
    private static final String JAR_FILE_ICON_PATH = "/icons/jar-file.png";
    private static final Color ICON_ENABLED_COLOR = new Color(0xEBEBEB);
    private static final Color ICON_DISABLED_COLOR = new Color(0x888888);
    private static final Color CLEAR_ICON_ENABLED_COLOR = new Color(0xE57373);
    private static final Color PRIMARY_ICON_ENABLED_COLOR = new Color(0x0A2E1C);
    public static final String LIST_HOVERED_INDEX_KEY = "mmremover.listHoveredIndex";
    public static final String LIST_DIRECTORY_PATH_KEY = "mmremover.listDirectoryPath";
    public static final int LIST_DELETE_ZONE_WIDTH = 36;
    public static final Color LIST_ITEM_HOVER_BACKGROUND = new Color(0x46494B);
    public static JButton createStyledButton(String text, Icon icon) {
        JButton button = new JButton(text, icon);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(46, 139, 87));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        });

        return button;
    }

    public static void enableListItemHover(JList<?> list) {
        list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseMotionAdapter motionAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateListHoveredIndex(list, list.locationToIndex(e.getPoint()));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateListHoveredIndex(list, list.locationToIndex(e.getPoint()));
            }
        };
        list.addMouseMotionListener(motionAdapter);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                updateListHoveredIndex(list, -1);
            }
        });
    }

    private static void updateListHoveredIndex(JList<?> list, int newIndex) {
        Integer oldIndex = (Integer) list.getClientProperty(LIST_HOVERED_INDEX_KEY);
        int previousIndex = oldIndex != null ? oldIndex : -1;

        if (newIndex < 0) {
            if (previousIndex >= 0) {
                list.putClientProperty(LIST_HOVERED_INDEX_KEY, null);
                repaintListCell(list, previousIndex);
            }
            return;
        }

        if (previousIndex == newIndex) {
            return;
        }

        list.putClientProperty(LIST_HOVERED_INDEX_KEY, newIndex);
        if (previousIndex >= 0) {
            repaintListCell(list, previousIndex);
        }
        repaintListCell(list, newIndex);
    }

    private static void repaintListCell(JList<?> list, int index) {
        Rectangle bounds = list.getCellBounds(index, index);
        if (bounds != null) {
            list.repaint(bounds);
        }
    }

    public static JButton createIconButton(Icon icon, String tooltip) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setMargin(new Insets(2, 4, 2, 4));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        return button;
    }

    public static Icon getRefreshIcon() {
        return getRefreshIcon(18);
    }

    public static Icon getRefreshIcon(int size) {
        Icon resourceIcon = createTintedResourceIcon(
                REFRESH_ICON_PATH, size, ICON_ENABLED_COLOR, ICON_DISABLED_COLOR);
        if (resourceIcon != null) {
            return resourceIcon;
        }
        return createDrawnRefreshIcon(size);
    }

    public static Icon getEraserIcon() {
        return getEraserIcon(22);
    }

    public static Icon getEraserIcon(int size) {
        Icon resourceIcon = createTintedResourceIcon(
                ERASER_ICON_PATH, size, PRIMARY_ICON_ENABLED_COLOR, ICON_DISABLED_COLOR);
        if (resourceIcon != null) {
            return resourceIcon;
        }
        return null;
    }

    public static Icon getJarFileIcon() {
        return getJarFileIcon(20);
    }

    public static Icon getJarFileIcon(int size) {
        Icon resourceIcon = createTintedResourceIcon(
                JAR_FILE_ICON_PATH, size, ICON_ENABLED_COLOR, ICON_DISABLED_COLOR);
        if (resourceIcon != null) {
            return resourceIcon;
        }
        return UIManager.getIcon("FileView.fileIcon");
    }

    private static Icon createTintedResourceIcon(
            String resourcePath, int size, Color enabledTint, Color disabledTint) {
        URL url = UIManagerUtil.class.getResource(resourcePath);
        if (url == null) {
            return null;
        }

        Image source = new ImageIcon(url).getImage();
        Image enabledImage = tintAndScale(source, size, enabledTint);
        Image disabledImage = tintAndScale(source, size, disabledTint);

        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Image image = c != null && !c.isEnabled() ? disabledImage : enabledImage;
                g.drawImage(image, x, y, null);
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    private static Image tintAndScale(Image source, int size, Color tint) {
        BufferedImage tinted = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tinted.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(source, 0, 0, size, size, null);
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.setColor(tint);
        g2.fillRect(0, 0, size, size);
        g2.dispose();
        return tinted;
    }

    private static Icon createDrawnRefreshIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.isEnabled() ? ICON_ENABLED_COLOR : ICON_DISABLED_COLOR);
                g2.setStroke(new BasicStroke(Math.max(1.2f, size / 12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                float s = size / 16f;
                int cx = x + Math.round(8 * s);
                int cy = y + Math.round(8 * s);
                int r = Math.round(5 * s);

                g2.drawArc(cx - r, cy - r, r * 2, r * 2, 45, 270);

                int ax = cx + Math.round(4 * s);
                int ay = cy - Math.round(5 * s);
                g2.drawLine(ax, ay, ax + Math.round(3 * s), ay);
                g2.drawLine(ax, ay, ax, ay + Math.round(3 * s));

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static Icon getFolderIcon() {
        return getFolderIcon(16);
    }

    public static Icon getFolderIcon(int size) {
        Icon systemIcon = UIManager.getIcon("FileView.directoryIcon");
        if (systemIcon instanceof ImageIcon imageIcon) {
            if (imageIcon.getIconWidth() != size || imageIcon.getIconHeight() != size) {
                Image scaled = imageIcon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
            return imageIcon;
        }
        if (systemIcon != null) {
            return systemIcon;
        }

        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.isEnabled() ? new Color(0xEBEBEB) : new Color(0x888888));
                g2.setStroke(new BasicStroke(Math.max(1f, size / 14f)));

                float s = size / 16f;
                int tabX = x + Math.round(3 * s);
                int tabY = y + Math.round(4 * s);
                int tabW = Math.round(6 * s);
                int tabH = Math.round(3 * s);
                int bodyX = x + Math.round(3 * s);
                int bodyY = y + Math.round(6 * s);
                int bodyW = Math.round(10 * s);
                int bodyH = Math.round(8 * s);

                g2.drawRoundRect(tabX, tabY, tabW, tabH, Math.round(1 * s), Math.round(1 * s));
                g2.drawRoundRect(bodyX, bodyY, bodyW, bodyH, Math.round(2 * s), Math.round(2 * s));
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static Icon getTrashIcon() {
        return getTrashIcon(16);
    }

    public static Icon getTrashIcon(int size) {
        Icon resourceIcon = createTintedResourceIcon(
                BROOM_ICON_PATH, size, CLEAR_ICON_ENABLED_COLOR, ICON_DISABLED_COLOR);
        if (resourceIcon != null) {
            return resourceIcon;
        }
        return createDrawnTrashIcon(size);
    }

    private static Icon createDrawnTrashIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.isEnabled() ? CLEAR_ICON_ENABLED_COLOR : ICON_DISABLED_COLOR);
                g2.setStroke(new BasicStroke(Math.max(1f, size / 14f)));

                float s = size / 16f;
                int lidY = y + Math.round(4 * s);
                int bodyX = x + Math.round(4 * s);
                int bodyY = y + Math.round(5 * s);
                int bodyW = Math.round(8 * s);
                int bodyH = Math.round(9 * s);

                g2.drawLine(x + Math.round(3 * s), lidY, x + Math.round(13 * s), lidY);
                g2.drawLine(x + Math.round(6 * s), y + Math.round(2 * s), x + Math.round(10 * s), y + Math.round(2 * s));
                g2.drawRoundRect(bodyX, bodyY, bodyW, bodyH, Math.round(2 * s), Math.round(2 * s));

                int lineX1 = x + Math.round(6 * s);
                int lineX2 = x + Math.round(8 * s);
                int lineX3 = x + Math.round(10 * s);
                int lineTop = bodyY + Math.round(2 * s);
                int lineBottom = bodyY + bodyH - Math.round(2 * s);
                g2.drawLine(lineX1, lineTop, lineX1, lineBottom);
                g2.drawLine(lineX2, lineTop, lineX2, lineBottom);
                g2.drawLine(lineX3, lineTop, lineX3, lineBottom);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static JPanel createPanelHeader(String title, JButton... actionButtons) {
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 15));
        label.setForeground(PANEL_BORDER_COLOR);
        header.add(label, BorderLayout.WEST);

        if (actionButtons.length > 0) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            actions.setOpaque(false);
            for (JButton button : actionButtons) {
                actions.add(button);
            }
            header.add(actions, BorderLayout.EAST);
        }

        return header;
    }

    public static void styleContentPanel(JComponent component) {
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                new RoundedLineBorder(PANEL_BORDER_COLOR, 1, PANEL_BORDER_RADIUS)
        ));
    }

    public static void styleTitledBorder(JComponent component, String title) {
        Border titledBorder = BorderFactory.createTitledBorder(
                new RoundedLineBorder(PANEL_BORDER_COLOR, 1, PANEL_BORDER_RADIUS),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 15),
                PANEL_BORDER_COLOR
        );
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                titledBorder
        ));
    }

    private static final class RoundedLineBorder implements Border {
        private final Color color;
        private final int thickness;
        private final int radius;

        private RoundedLineBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            float offset = thickness / 2f;
            g2.draw(new RoundRectangle2D.Float(
                    x + offset,
                    y + offset,
                    width - thickness,
                    height - thickness,
                    radius,
                    radius
            ));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            int pad = thickness + 1;
            return new Insets(pad, pad, pad, pad);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

}
