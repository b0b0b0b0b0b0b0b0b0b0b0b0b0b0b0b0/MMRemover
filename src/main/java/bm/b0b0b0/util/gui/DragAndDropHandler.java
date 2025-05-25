package bm.b0b0b0.util.gui;

import bm.b0b0b0.util.files.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.nio.file.*;
import java.util.List;
import java.util.function.BiConsumer;

public class DragAndDropHandler {

    public static void addDragAndDropSupport(JList<String> list, JTextArea consoleArea, String folderName, BiConsumer<JList<String>, String> refreshFilesList, Conf conf) {
        new DropTarget(list, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {}

            @Override
            public void dragOver(DropTargetDragEvent dtde) {}

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {}

            @Override
            public void dragExit(DropTargetEvent dte) {}

            @Override
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent dtde) {
                try {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        List<File> droppedFiles = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                        Path targetDir = Paths.get(folderName).toAbsolutePath();
                        if (!Files.exists(targetDir)) {
                            Files.createDirectories(targetDir);
                            System.out.println(conf.getTranslation("directoryCreated") + ": " + targetDir);
                        }

                        System.out.println(conf.getTranslation("dropCurrentDir") + ": " + System.getProperty("user.dir"));
                        System.out.println(conf.getTranslation("copyingFilesToDir") + ": " + targetDir);

                        for (File file : droppedFiles) {
                            if (file.getName().endsWith(".jar")) {
                                Path destFile = targetDir.resolve(file.getName());
                                try {
                                    Files.copy(file.toPath(), destFile, StandardCopyOption.REPLACE_EXISTING);
                                    FileUtils.appendToConsole(consoleArea, conf.getTranslation("fileAdded") + ": " + file.getName());
                                    System.out.println(conf.getTranslation("fileCopied") + ": " + destFile);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    FileUtils.appendToConsole(consoleArea, conf.getTranslation("errorCopyingFile") + ": " + file.getName() + " -> " + destFile);
                                    System.out.println(conf.getTranslation("errorCopyingFile") + ": " + file.getName() + " -> " + destFile);
                                }
                            } else {
                                FileUtils.appendToConsole(consoleArea, conf.getTranslation("fileIgnored") + ": " + file.getName());
                            }
                        }
                        refreshFilesList.accept(list, targetDir.toString());
                        dtde.dropComplete(true);
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    FileUtils.appendToConsole(consoleArea, conf.getTranslation("errorProcessingFiles") + ": " + e.getMessage());
                    dtde.dropComplete(false);
                }
            }
        });
    }



    public static void addDragAndDropSupport(JList<String> list) {
        list.setDragEnabled(true);
        list.setTransferHandler(new TransferHandler() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                JList<?> list = (JList<?>) c;
                List<String> selectedValues = (List<String>) list.getSelectedValuesList();
                return new Transferable() {
                    @Override
                    public DataFlavor[] getTransferDataFlavors() {
                        return new DataFlavor[]{DataFlavor.javaFileListFlavor};
                    }

                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return DataFlavor.javaFileListFlavor.equals(flavor);
                    }

                    @Override
                    public Object getTransferData(DataFlavor flavor) {
                        return selectedValues.stream()
                                .map(name -> new File("out", name))
                                .toList();
                    }
                };
            }

            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.COPY;
            }
        });
    }
}
