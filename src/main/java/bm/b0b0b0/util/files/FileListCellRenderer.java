package bm.b0b0b0.util.files;

import bm.b0b0b0.util.gui.Conf;
import bm.b0b0b0.util.gui.MMRemoverTheme;
import bm.b0b0b0.util.gui.UIManagerUtil;
import bm.b0b0b0.util.log.AppLogger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class FileListCellRenderer extends JPanel implements ListCellRenderer<Object> {
    private static final int ROW_ARC = 10;

    private final String directoryPath;
    private final Conf conf;
    private final JLabel nameLabel = new JLabel();
    private final JLabel sizeLabel = new JLabel();
    private final JLabel deleteLabel = new JLabel();
    private final Icon jarIcon = UIManagerUtil.getJarFileIcon();
    private final Icon folderIcon = UIManagerUtil.getFolderIcon();
    private Color rowBackground;
    private boolean selectedRow;

    public FileListCellRenderer(String directoryPath, Conf conf) {
        super(new BorderLayout(10, 0));
        this.directoryPath = directoryPath;
        this.conf = conf;
        setOpaque(false);
        setBorder(new EmptyBorder(0, 10, 0, 4));

        nameLabel.setIconTextGap(10);

        sizeLabel.putClientProperty("FlatLaf.style", "font: -1");
        sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        deleteLabel.setIcon(UIManagerUtil.getBinIcon(15));
        deleteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        deleteLabel.setPreferredSize(new Dimension(UIManagerUtil.LIST_DELETE_ZONE_WIDTH - 4, 0));
        deleteLabel.setToolTipText(conf.getTranslation("deleteSelectedFileTooltip"));

        JPanel east = new JPanel(new BorderLayout(6, 0));
        east.setOpaque(false);
        east.add(sizeLabel, BorderLayout.CENTER);
        east.add(deleteLabel, BorderLayout.EAST);

        add(nameLabel, BorderLayout.CENTER);
        add(east, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        String fileName = value.toString();
        String filePath = resolveDirectoryPath(list) + File.separator + fileName;
        boolean isJar = fileName.endsWith(".jar");

        nameLabel.setText(fileName);
        nameLabel.setFont(list.getFont());
        nameLabel.setForeground(list.getForeground());
        nameLabel.setIcon(isJar ? jarIcon : folderIcon);
        sizeLabel.setForeground(MMRemoverTheme.mutedForeground());
        sizeLabel.setText("");

        if (isJar) {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                sizeLabel.setText(getReadableFileSize(file.length()));
            } else {
                AppLogger.get().warn(conf.getTranslation("fileNotFoundOrNotFile"));
            }
        }

        Integer hoveredIndex = (Integer) list.getClientProperty(UIManagerUtil.LIST_HOVERED_INDEX_KEY);
        boolean isHovered = hoveredIndex != null && hoveredIndex == index;
        selectedRow = isSelected;
        if (isSelected) {
            rowBackground = MMRemoverTheme.selectedBackground();
        } else if (isHovered) {
            rowBackground = MMRemoverTheme.hoverBackground();
        } else {
            rowBackground = null;
        }
        deleteLabel.setVisible(isSelected && isJar);
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (rowBackground == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(rowBackground);
        g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, ROW_ARC, ROW_ARC);
        if (selectedRow) {
            g2.setColor(MMRemoverTheme.accentColor());
            g2.fillRoundRect(0, 9, 3, getHeight() - 18, 3, 3);
        }
        g2.dispose();
    }

    private String resolveDirectoryPath(JList<?> list) {
        String pathFromList = (String) list.getClientProperty(UIManagerUtil.LIST_DIRECTORY_PATH_KEY);
        return pathFromList != null ? pathFromList : directoryPath;
    }

    private String getReadableFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return (size / 1024) + " KB";
        }
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }
}
