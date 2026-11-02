package bm.b0b0b0.util.gui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.ui.FlatLineBorder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.function.Supplier;

public class UIManagerUtil {
    private static final String SVG_ICON_DIR = "icons/svg/";
    private static final int CARD_ARC = 14;
    public static final String PANEL_TITLE_LABEL_KEY = "mmremover.panelTitleLabel";
    public static final String PANEL_COUNT_LABEL_KEY = "mmremover.panelCountLabel";
    public static final String LIST_HOVERED_INDEX_KEY = "mmremover.listHoveredIndex";
    public static final String LIST_DIRECTORY_PATH_KEY = "mmremover.listDirectoryPath";
    public static final int LIST_DELETE_ZONE_WIDTH = 36;

    public static Icon svgIcon(String name, int size, Supplier<Color> tint) {
        FlatSVGIcon icon = new FlatSVGIcon(SVG_ICON_DIR + name + ".svg", size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
            Color target = tint.get();
            if (target == null) {
                return color;
            }
            return new Color(target.getRed(), target.getGreen(), target.getBlue(),
                    color.getAlpha() * target.getAlpha() / 255);
        }));
        return icon;
    }

    private static Icon neutralIcon(String name, int size) {
        return svgIcon(name, size, () -> UIManager.getColor("MMRemover.iconColor"));
    }

    private static Icon dangerIcon(String name, int size) {
        return svgIcon(name, size, MMRemoverTheme::dangerColor);
    }

    public static void enableListItemHover(JList<?> list) {
        MouseMotionAdapter motionAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateListHoveredIndex(list, rowAt(list, e.getPoint()));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateListHoveredIndex(list, rowAt(list, e.getPoint()));
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

    private static int rowAt(JList<?> list, Point point) {
        int index = list.locationToIndex(point);
        if (index < 0) {
            return -1;
        }
        Rectangle bounds = list.getCellBounds(index, index);
        boolean inside = bounds != null && bounds.contains(point);
        list.setCursor(Cursor.getPredefinedCursor(inside ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        return inside ? index : -1;
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

    /**
     * Список файлов, который показывает подсказку, пока он пуст.
     */
    public static JList<String> createFileList(String emptyHint) {
        JList<String> list = new JList<>() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getModel().getSize() == 0 && emptyHint != null) {
                    paintEmptyHint(this, (Graphics2D) g, emptyHint);
                }
            }
        };
        list.setBorder(new EmptyBorder(2, 4, 4, 4));
        list.setFixedCellHeight(38);
        return list;
    }

    private static void paintEmptyHint(JComponent c, Graphics2D g, String text) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Icon icon = svgIcon("upload", 28, MMRemoverTheme::mutedForeground);
        FontMetrics fm = g2.getFontMetrics(c.getFont());
        int textWidth = fm.stringWidth(text);
        int blockHeight = icon.getIconHeight() + 10 + fm.getHeight();
        int top = Math.max(8, (c.getHeight() - blockHeight) / 2);
        icon.paintIcon(c, g2, (c.getWidth() - icon.getIconWidth()) / 2, top);
        g2.setFont(c.getFont());
        g2.setColor(MMRemoverTheme.mutedForeground());
        g2.drawString(text, (c.getWidth() - textWidth) / 2, top + icon.getIconHeight() + 10 + fm.getAscent());
        g2.dispose();
    }

    public static JButton createIconButton(Icon icon, String tooltip) {
        JButton button = new TipButton(icon);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.putClientProperty("FlatLaf.style", "arc: 8; margin: 5,5,5,5");
        return button;
    }

    public static Icon getRefreshIcon() {
        return getRefreshIcon(16);
    }

    public static Icon getRefreshIcon(int size) {
        return neutralIcon("refresh", size);
    }

    public static Icon getEraserIcon() {
        return getEraserIcon(16);
    }

    public static Icon getEraserIcon(int size) {
        return svgIcon("sparkles", size, () -> Color.WHITE);
    }

    public static Icon getJarFileIcon() {
        return getJarFileIcon(18);
    }

    public static Icon getJarFileIcon(int size) {
        return svgIcon("package", size, () -> UIManager.getColor("MMRemover.jarIconColor"));
    }

    public static Icon getInspectorIcon() {
        return getInspectorIcon(16);
    }

    public static Icon getInspectorIcon(int size) {
        return neutralIcon("search", size);
    }

    public static Icon getFolderIcon() {
        return getFolderIcon(16);
    }

    public static Icon getFolderIcon(int size) {
        return neutralIcon("folder-open", size);
    }

    public static Icon getTrashIcon() {
        return getTrashIcon(16);
    }

    public static Icon getTrashIcon(int size) {
        return dangerIcon("ban", size);
    }

    public static Icon getBinIcon() {
        return getBinIcon(16);
    }

    public static Icon getBinIcon(int size) {
        return dangerIcon("trash", size);
    }

    public static Icon getBackupIcon() {
        return getBackupIcon(16);
    }

    public static Icon getBackupIcon(int size) {
        return neutralIcon("archive", size);
    }

    public static Icon getCopyIcon() {
        return getCopyIcon(16);
    }

    public static Icon getCopyIcon(int size) {
        return neutralIcon("copy", size);
    }

    public static Icon getLogFileIcon() {
        return getLogFileIcon(16);
    }

    public static Icon getLogFileIcon(int size) {
        return neutralIcon("file-text", size);
    }

    public static Icon getLogsFolderIcon() {
        return getLogsFolderIcon(16);
    }

    public static Icon getLogsFolderIcon(int size) {
        return neutralIcon("folder-logs", size);
    }

    public static Icon getClearLogArchivesIcon() {
        return getClearLogArchivesIcon(16);
    }

    public static Icon getClearLogArchivesIcon(int size) {
        return dangerIcon("files-x", size);
    }

    public static Icon getGlobeIcon(int size) {
        return neutralIcon("globe", size);
    }

    public static JPanel createPanelHeader(String title, JButton... actionButtons) {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 4, 6, 0));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleBox.setOpaque(false);

        JLabel label = new JLabel(title);
        label.putClientProperty("FlatLaf.style", "font: bold +1");
        titleBox.add(label);

        JLabel count = new BadgeLabel();
        count.setVisible(false);
        titleBox.add(Box.createHorizontalStrut(8));
        titleBox.add(count);

        header.putClientProperty(PANEL_TITLE_LABEL_KEY, label);
        header.putClientProperty(PANEL_COUNT_LABEL_KEY, count);
        label.putClientProperty(PANEL_COUNT_LABEL_KEY, count);

        JPanel titleWrap = new JPanel(new GridBagLayout());
        titleWrap.setOpaque(false);
        titleWrap.add(titleBox);
        header.add(titleWrap, BorderLayout.WEST);

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

    /**
     * Скруглённая карточка с тонкой рамкой.
     */
    public static void styleContentPanel(JComponent component) {
        styleContentPanel(component, MMRemoverTheme.cardBackground());
    }

    public static void styleContentPanel(JComponent component, Color background) {
        // FlatPanelUI рисует скруглённый фон только у непрозрачной панели
        // и только не-UIResource цвет: UIResource-фон FlatLaf перезаписывает своим
        component.setOpaque(true);
        component.setBackground(new Color(background.getRGB(), true));
        component.putClientProperty("FlatLaf.style", "arc: " + CARD_ARC);
        component.setBorder(new FlatLineBorder(
                new Insets(10, 10, 10, 10), MMRemoverTheme.cardBorderColor(), 1, CARD_ARC));
    }

    /**
     * Небольшая «таблетка» со счётчиком рядом с заголовком панели.
     */
    private static final class BadgeLabel extends JLabel {
        BadgeLabel() {
            setBorder(new EmptyBorder(1, 8, 1, 8));
            putClientProperty("FlatLaf.style", "font: -1");
        }

        @Override
        public Color getForeground() {
            Color muted = MMRemoverTheme.mutedForeground();
            return muted != null ? muted : super.getForeground();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(MMRemoverTheme.hoverBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
