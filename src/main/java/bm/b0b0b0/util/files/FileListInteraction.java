package bm.b0b0b0.util.files;

import bm.b0b0b0.util.gui.Conf;
import bm.b0b0b0.util.gui.UIManagerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public final class FileListInteraction {
    private FileListInteraction() {
    }

    public static void enableFileDelete(JList<String> list, JTextArea consoleArea, Conf conf, Runnable onDeleted) {
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 1) {
                    return;
                }

                int index = list.locationToIndex(e.getPoint());
                if (index < 0 || list.getSelectedIndex() != index) {
                    return;
                }

                String fileName = list.getSelectedValue();
                if (fileName == null || !fileName.endsWith(".jar")) {
                    return;
                }

                Rectangle cellBounds = list.getCellBounds(index, index);
                if (cellBounds == null) {
                    return;
                }

                Rectangle deleteZone = new Rectangle(
                        cellBounds.x + cellBounds.width - UIManagerUtil.LIST_DELETE_ZONE_WIDTH,
                        cellBounds.y,
                        UIManagerUtil.LIST_DELETE_ZONE_WIDTH,
                        cellBounds.height
                );
                if (!deleteZone.contains(e.getPoint())) {
                    return;
                }

                String directoryPath = (String) list.getClientProperty(UIManagerUtil.LIST_DIRECTORY_PATH_KEY);
                if (directoryPath == null) {
                    return;
                }

                Window parent = SwingUtilities.getWindowAncestor(list);
                if (!confirmDelete(parent, conf, fileName)) {
                    return;
                }

                File file = new File(directoryPath, fileName);
                if (FileUtils.deleteFile(file, consoleArea, conf)) {
                    onDeleted.run();
                }
            }
        });
    }

    private static boolean confirmDelete(Window parent, Conf conf, String fileName) {
        Object[] options = {
                conf.getTranslation("confirmYes"),
                conf.getTranslation("confirmNo")
        };
        int option = JOptionPane.showOptionDialog(
                parent,
                String.format(conf.getTranslation("confirmDeleteFile"), fileName),
                conf.getTranslation("confirmActionTitle"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
        );
        return option == JOptionPane.YES_OPTION;
    }
}
