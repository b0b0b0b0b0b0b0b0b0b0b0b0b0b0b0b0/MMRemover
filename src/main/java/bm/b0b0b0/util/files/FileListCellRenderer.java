package bm.b0b0b0.util.files;

import bm.b0b0b0.util.gui.Conf;
import bm.b0b0b0.util.gui.UIManagerUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FileListCellRenderer extends DefaultListCellRenderer {
    private final String directoryPath;
    private final Color selectedBackgroundColor = Color.decode("#16181c");
    private final Conf conf;

    public FileListCellRenderer(String directoryPath, Conf conf) {
        this.directoryPath = directoryPath;
        this.conf = conf;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        label.setFont(new Font("Arial", Font.BOLD, 14));

        String fileName = value.toString();
        String resolvedDirectoryPath = resolveDirectoryPath(list);
        String filePath = resolvedDirectoryPath + File.separator + fileName;

        if (fileName.endsWith(".jar")) {
            label.setIcon(UIManagerUtil.getJarFileIcon());

            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                long fileSize = file.length();
                String sizeText = getReadableFileSize(fileSize);
                label.setText("<html>" + fileName + "<br><font size='2' color='gray'>[" + sizeText + "]</font></html>");
            } else {
                System.out.println(conf.getTranslation("fileNotFoundOrNotFile"));
            }
        } else {
            label.setIcon(UIManager.getIcon("FileView.directoryIcon"));
        }

        applyBackground(label, list, index, isSelected);

        if (isSelected && fileName.endsWith(".jar")) {
            JPanel panel = new JPanel(new BorderLayout(4, 0));
            panel.setOpaque(true);
            panel.setBackground(label.getBackground());
            panel.setForeground(label.getForeground());
            label.setOpaque(false);
            label.setBackground(null);
            panel.add(label, BorderLayout.CENTER);

            JLabel deleteIcon = new JLabel(UIManagerUtil.getTrashIcon(14));
            deleteIcon.setToolTipText(conf.getTranslation("deleteSelectedFileTooltip"));
            deleteIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
            panel.add(deleteIcon, BorderLayout.EAST);
            return panel;
        }

        return label;
    }

    private String resolveDirectoryPath(JList<?> list) {
        String pathFromList = (String) list.getClientProperty(UIManagerUtil.LIST_DIRECTORY_PATH_KEY);
        return pathFromList != null ? pathFromList : directoryPath;
    }

    private void applyBackground(JComponent component, JList<?> list, int index, boolean isSelected) {
        if (isSelected) {
            component.setBackground(selectedBackgroundColor);
            component.setOpaque(true);
        } else {
            Integer hoveredIndex = (Integer) list.getClientProperty(UIManagerUtil.LIST_HOVERED_INDEX_KEY);
            boolean isHovered = hoveredIndex != null && hoveredIndex == index;
            if (isHovered) {
                component.setBackground(UIManagerUtil.LIST_ITEM_HOVER_BACKGROUND);
                component.setOpaque(true);
            } else {
                component.setBackground(list.getBackground());
                component.setOpaque(false);
            }
        }
    }

    private String getReadableFileSize(long size) {
        long sizeInKB = size / 1024;
        return sizeInKB + " KB";
    }
}
