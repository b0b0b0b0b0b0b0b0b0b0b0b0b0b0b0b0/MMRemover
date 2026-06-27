package bm.b0b0b0.util.files;

import bm.b0b0b0.util.gui.Conf;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class FileUtils {

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static String formatLogLine(String message) {
        return "[" + LocalTime.now().format(LOG_TIME) + "] " + message;
    }

    public static void appendToConsole(JTextArea consoleArea, String message) {
        consoleArea.append(formatLogLine(message) + "\n");
        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
    }

    public static void openFolder(String folderName, JTextArea consoleArea, Conf conf) {
        File folder = new File(folderName);
        if (folder.exists() && folder.isDirectory()) {
            try {
                Desktop.getDesktop().open(folder);
                appendToConsole(consoleArea, String.format(conf.getTranslation("folderOpened"), folder.getName()));
            } catch (IOException e) {
                appendToConsole(consoleArea, String.format(conf.getTranslation("errorOpeningFolder"), folder.getName()));
            }
        } else {
            appendToConsole(consoleArea, String.format(conf.getTranslation("folderNotFound"), folder.getName()));
        }
    }

    public static void clearFolder(Path folderPath, JTextArea consoleArea, Conf conf) {
        File folder = folderPath.toFile();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    deleteFile(file, consoleArea, conf);
                }
            }
        }
    }

    public static boolean deleteFile(File file, JTextArea consoleArea, Conf conf) {
        if (file.exists() && file.isFile() && file.delete()) {
            appendToConsole(consoleArea, String.format(conf.getTranslation("fileDeleted"), file.getName()));
            return true;
        }
        appendToConsole(consoleArea, String.format(conf.getTranslation("errorDeletingFile"), file.getName()));
        return false;
    }


    public static String generateUniqueFileName(File directory, String baseName) {
        String fileName = baseName;
        int counter = 1;
        while (new File(directory, fileName).exists()) {
            int dotIndex = baseName.lastIndexOf('.');
            if (dotIndex == -1) {
                fileName = baseName + "_" + counter;
            } else {
                fileName = baseName.substring(0, dotIndex) + "_" + counter + baseName.substring(dotIndex);
            }
            counter++;
        }
        return fileName;
    }
}
