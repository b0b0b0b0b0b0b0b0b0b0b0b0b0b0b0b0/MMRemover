package bm.b0b0b0.util.files;

import bm.b0b0b0.hacks.*;
import bm.b0b0b0.util.gui.Conf;

import javax.swing.JTextArea;
import java.io.File;
import java.nio.file.Path;

public class FileProcessor {

    public static void processFiles(Path inputPath, Path outputPath, String type, JTextArea consoleArea, Conf conf) {
        FileUtils.appendToConsole(consoleArea, conf.getTranslation("fileProcessingStart"));
        File inputFolder = inputPath.toFile();
        File outputFolder = outputPath.toFile();

        try {
            if (inputFolder.exists() && inputFolder.isDirectory()) {
                if (!outputFolder.exists()) {
                    if (outputFolder.mkdirs()) {
                        FileUtils.appendToConsole(consoleArea, conf.getTranslation("outputFolderCreated"));
                    } else {
                        FileUtils.appendToConsole(consoleArea, conf.getTranslation("outputFolderCreationError"));
                        return;
                    }
                }

                if (outputFolder.isDirectory()) {
                    if ("hostflow".equalsIgnoreCase(type)) {
                        new HostFlow(inputPath, outputPath, true, consoleArea, conf);
                    } else if ("artemka".equalsIgnoreCase(type)) {
                        new Artemka(inputPath, outputPath, true, consoleArea, conf);
                    } else if ("bstatsjar".equalsIgnoreCase(type)) {
                        new bStatsJar(inputPath, outputPath, true, consoleArea, conf);
                    } else if ("pluginmetrics".equalsIgnoreCase(type)) {
                        new PluginMetrics(inputPath, outputPath, true, consoleArea, conf);
                    } else if ("aph".equalsIgnoreCase(type)) {
                        new aph(inputPath, outputPath, true, consoleArea, conf);
                    } else if ("chbkHack".equalsIgnoreCase(type)) {
                        new chbkHack(inputPath, outputPath, true, consoleArea, conf);
                    } else if ("ruBstatsHack".equalsIgnoreCase(type)) {
                        new ruBstatsHack(inputPath, outputPath, true, consoleArea, conf);
                    }

                    FileUtils.appendToConsole(consoleArea, conf.getTranslation("processingCompleted"));
                } else {
                    FileUtils.appendToConsole(consoleArea, conf.getTranslation("errorNotADirectoryOut"));
                }
            } else {
                FileUtils.appendToConsole(consoleArea, conf.getTranslation("errorNotADirectoryInput"));
            }
        } catch (Exception e) {
            FileUtils.appendToConsole(consoleArea, conf.getTranslation("errorProcessingFiles") + e.getMessage());
            e.printStackTrace();
        }
    }
}

