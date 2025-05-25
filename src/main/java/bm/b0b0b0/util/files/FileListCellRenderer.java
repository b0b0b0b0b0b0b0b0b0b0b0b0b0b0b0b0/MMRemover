package bm.b0b0b0.util.files;

import bm.b0b0b0.util.gui.Conf;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import bm.b0b0b0.util.gui.Conf;
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
        String filePath = directoryPath + File.separator + fileName;

        if (fileName.endsWith(".jar")) {
            label.setIcon(UIManager.getIcon("FileView.fileIcon"));

            File file = new File(filePath);
            //System.out.println(String.format(conf.getTranslation("fileCheck"), file.getAbsolutePath()));

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

        if (isSelected) {
            label.setBackground(selectedBackgroundColor);
            label.setOpaque(true);
        } else {
            label.setBackground(list.getBackground());
            label.setOpaque(false);
        }

        return label;
    }

    private String getReadableFileSize(long size) {
        long sizeInKB = size / 1024;
        return sizeInKB + " KB";
    }
}
