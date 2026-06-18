package bm.b0b0b0;

import bm.b0b0b0.util.gui.Conf;
import bm.b0b0b0.util.gui.DragAndDropHandler;
import bm.b0b0b0.util.gui.StyledButton;
import bm.b0b0b0.util.files.FileListCellRenderer;
import bm.b0b0b0.util.files.FileProcessor;
import bm.b0b0b0.util.files.FileUtils;
import bm.b0b0b0.util.gui.UIManagerUtil;
import bm.b0b0b0.util.gui.LanguageSelector;
import bm.b0b0b0.util.gui.load.LoadingDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class MainWindow extends JFrame {
    private JList<String> inputList;
    private JList<String> outputList;
    private Path pluginsDir = Paths.get("input");
    private JTextArea consoleArea;
    private final Conf conf;

    private static final Color COLOR_MAIN_BG         = Color.decode("#3c3f41");
    private static final Color COLOR_CONSOLE_BG      = Color.BLACK;
    private static final Color COLOR_CONSOLE_FG      = Color.WHITE;
    private static final Color COLOR_DELETE_BG       = new Color(255, 77, 77);
    private static final Color COLOR_PANEL_TITLE_BG  = Color.decode("#3c3f41");
    private static final Color COLOR_LINK_PANEL_BG   = Color.decode("#3c3f41");
    private static final Color COLOR_BUTTON_BG       = Color.decode("#3c3f41");
    private static final Color COLOR_VISIT_LINK_FG   = Color.decode("#09b55d");


    public MainWindow(Conf conf) {
        this.conf = conf;
        initializeUI();
        setVisible(true);
    }

    private void initializeUI() {

        setTitle("MMRemover (v1.15)");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setWorkingDirectoryToJarLocation();
        setAppIcon();
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception e) {
            System.err.println(conf.getTranslation("no_black") + ": " + e.getMessage());
        }
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(COLOR_MAIN_BG);
        add(mainPanel);
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(COLOR_MAIN_BG);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(COLOR_BUTTON_BG);
        JButton removeAllButton = StyledButton.createModernButton(conf.getTranslation("removeAllButton"), null);
        buttonPanel.add(removeAllButton);
        topPanel.add(buttonPanel);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(COLOR_PANEL_TITLE_BG);
        inputList = new JList<>();
        inputList.setCellRenderer(new FileListCellRenderer(new File(pluginsDir.toString()).getAbsolutePath(), conf));
        JScrollPane inputScrollPane = new JScrollPane(inputList);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        UIManagerUtil.styleTitledBorder(inputPanel, conf.getTranslation("inputPanelTitle"));
        inputPanel.setPreferredSize(new Dimension(350, 250));
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBackground(COLOR_PANEL_TITLE_BG);
        outputList = new JList<>();
        outputList.setCellRenderer(new FileListCellRenderer(new File("out").getAbsolutePath(), conf));
        JScrollPane outputScrollPane = new JScrollPane(outputList);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);
        UIManagerUtil.styleTitledBorder(outputPanel, conf.getTranslation("outputPanelTitle"));
        outputPanel.setPreferredSize(new Dimension(350, 250));
        JSplitPane fileSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, outputPanel);
        fileSplitPane.setDividerLocation(350);
        fileSplitPane.setBackground(COLOR_MAIN_BG);
        consoleArea = new JTextArea(5, 20);
        consoleArea.setBackground(COLOR_CONSOLE_BG);
        consoleArea.setForeground(COLOR_CONSOLE_FG);
        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        UIManagerUtil.styleTitledBorder(consoleScrollPane, conf.getTranslation("consolePanelTitle"));
        consoleScrollPane.setMinimumSize(new Dimension(0, 150));
        consoleScrollPane.setPreferredSize(new Dimension(800, 200));
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, fileSplitPane, consoleScrollPane);
        mainSplitPane.setDividerLocation(350);
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(COLOR_MAIN_BG);
        JPanel secondaryButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        secondaryButtonPanel.setBackground(COLOR_BUTTON_BG);
        JButton refreshFilesButton = UIManagerUtil.createStyledButton(conf.getTranslation("refreshFilesButton"), UIManager.getIcon("FileView.refreshIcon"));
        JButton openInputFolderButton = UIManagerUtil.createStyledButton(conf.getTranslation("openInputFolderButton"), UIManager.getIcon("FileView.directoryIcon"));
        JButton openOutputFolderButton = UIManagerUtil.createStyledButton(conf.getTranslation("openOutputFolderButton"), UIManager.getIcon("FileView.directoryIcon"));
        JButton clearInputListButton = UIManagerUtil.createStyledButton(conf.getTranslation("clearInputListButton"), UIManager.getIcon("FileView.floppyDriveIcon"));
        JButton clearOutputListButton = UIManagerUtil.createStyledButton(conf.getTranslation("clearOutputListButton"), UIManager.getIcon("FileView.floppyDriveIcon"));
        JButton clearConsoleButton = UIManagerUtil.createStyledButton(conf.getTranslation("clearConsoleButton"), UIManager.getIcon("FileView.computerIcon"));
        clearInputListButton.setBackground(COLOR_DELETE_BG);
        clearInputListButton.setForeground(Color.WHITE);

        clearOutputListButton.setBackground(COLOR_DELETE_BG);
        clearOutputListButton.setForeground(Color.WHITE);

        clearConsoleButton.setBackground(COLOR_DELETE_BG);
        clearConsoleButton.setForeground(Color.WHITE);

        secondaryButtonPanel.add(refreshFilesButton);
        secondaryButtonPanel.add(openInputFolderButton);
        secondaryButtonPanel.add(openOutputFolderButton);
        secondaryButtonPanel.add(clearInputListButton);
        secondaryButtonPanel.add(clearOutputListButton);
        secondaryButtonPanel.add(clearConsoleButton);

        JPanel linkPanel = new JPanel(new BorderLayout());
        linkPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        linkPanel.setBackground(COLOR_LINK_PANEL_BG);

        JButton visitWebsiteButton = new JButton("<HTML><U>" + conf.getTranslation("pageTitle") + "</U></HTML>");
        visitWebsiteButton.setForeground(COLOR_VISIT_LINK_FG);
        visitWebsiteButton.setBorderPainted(false);
        visitWebsiteButton.setOpaque(false);
        visitWebsiteButton.setBackground(null);
        visitWebsiteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/bm.png")));
            Image img = icon.getImage();
            Image scaledImg = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImg);
            visitWebsiteButton.setIcon(scaledIcon);
        } catch (Exception e) {
            System.err.println(conf.getTranslation("Icon_not_found") + ": " + e.getMessage());
        }

        JButton botCheckButton = new JButton("<HTML><U>" + conf.getTranslation("bot_bm") + "</U></HTML>");
        botCheckButton.setForeground(COLOR_VISIT_LINK_FG);
        botCheckButton.setBorderPainted(false);
        botCheckButton.setOpaque(false);
        botCheckButton.setBackground(null);
        botCheckButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/bot.png")));
            Image img = icon.getImage();
            Image scaledImg = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImg);
            botCheckButton.setIcon(scaledIcon);
        } catch (Exception e) {
            System.err.println("Icon not found: " + e.getMessage());
        }

        botCheckButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://bm.wtf/bot"));
            } catch (IOException | URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        visitWebsiteButton.setVerticalTextPosition(SwingConstants.CENTER);
        visitWebsiteButton.setVerticalAlignment(SwingConstants.CENTER);

        visitWebsiteButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://black-minecraft.com/resources/6091/"));
            } catch (IOException | URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        JPanel linksBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        linksBox.setOpaque(false);
        linksBox.add(botCheckButton);
        linksBox.add(visitWebsiteButton);

        linkPanel.add(linksBox, BorderLayout.EAST);
        bottomPanel.add(secondaryButtonPanel, BorderLayout.CENTER);
        bottomPanel.add(linkPanel, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        DragAndDropHandler.addDragAndDropSupport(inputList, consoleArea, pluginsDir.toString(), this::refreshFilesList, conf);
        DragAndDropHandler.addDragAndDropSupport(outputList);
        refreshFilesButton.addActionListener(e -> {
            logCurrentDirectory(conf.getTranslation("refreshFiles"));
            refreshFilesList(inputList, pluginsDir.toString());
            refreshFilesList(outputList, "out");
        });

        removeAllButton.addActionListener(e -> {
            long startTime = System.currentTimeMillis();
            LoadingDialog loadingDialog = new LoadingDialog(this, conf);
            loadingDialog.setModal(false);

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    logCurrentDirectory("Запуск очистки для всех типов вирусов...");
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "hostflow", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "artemka", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "bstatsjar", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "pluginmetrics", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "aph", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "chbkHack", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "ruBstatsHack", consoleArea, conf);
                    return null;
                }

                @Override
                protected void done() {
                    long elapsed = System.currentTimeMillis() - startTime;
                    long minDuration = 3500;

                    if (elapsed < minDuration) {
                        long remain = minDuration - elapsed;
                        Timer t = new Timer((int) remain, actionEvent -> {
                            loadingDialog.dispose();
                            refreshFilesList(outputList, "out");
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        loadingDialog.dispose();
                        refreshFilesList(outputList, "out");
                    }
                }
            };

            worker.execute();
            loadingDialog.setVisible(true);
        });

        JButton chooseFolderButton = StyledButton.createModernButton(conf.getTranslation("chooseFolderButton"), null);
        buttonPanel.add(chooseFolderButton);
        chooseFolderButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pluginsDir = fc.getSelectedFile().toPath();
                logCurrentDirectory("Выбрана папка: " + pluginsDir);
                refreshFilesList(inputList, pluginsDir.toString());
                DragAndDropHandler.addDragAndDropSupport(inputList, consoleArea, pluginsDir.toString(), this::refreshFilesList, conf);
            }
        });


        openInputFolderButton.addActionListener(e -> {
            logCurrentDirectory(conf.getTranslation("openInputFolder"));
            FileUtils.openFolder(pluginsDir.toString(), consoleArea, conf);
        });

        openOutputFolderButton.addActionListener(e -> {
            logCurrentDirectory(conf.getTranslation("openOutputFolder"));
            FileUtils.openFolder("out", consoleArea, conf);
        });

        clearInputListButton.addActionListener(e -> {
            logCurrentDirectory(conf.getTranslation("clearInputList"));
            if (confirmAction(conf.getTranslation("confirmClearInputList"))) {
                clearFilesList(inputList);
                FileUtils.clearFolder(new File(pluginsDir.toString()).toPath(), consoleArea, conf);
            }
        });

        clearOutputListButton.addActionListener(e -> {
            logCurrentDirectory(conf.getTranslation("clearOutputList"));
            if (confirmAction(conf.getTranslation("confirmClearOutputList"))) {
                clearFilesList(outputList);
                FileUtils.clearFolder(new File("out").toPath(), consoleArea, conf);
            }
        });

        clearConsoleButton.addActionListener(e -> {
            if (confirmAction(conf.getTranslation("confirmClearConsole"))) {
                clearConsole();
            }
        });

        logCurrentDirectory(conf.getTranslation("rel_file_start"));
        refreshFilesList(inputList, pluginsDir.toString());
        refreshFilesList(outputList, "out");
        topPanel.revalidate();
        topPanel.repaint();
        mainPanel.revalidate();
        mainPanel.repaint();
        pack();

        setVisible(true);
    }


    private void setWorkingDirectoryToJarLocation() {
        try {
            File jarFile = new File(MainWindow.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            System.setProperty("user.dir", jarFile.getParent());
            System.out.println(conf.getTranslation("workingDirSet") + System.getProperty("user.dir"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.err.println(conf.getTranslation("workingDirFailed"));
        }
    }

    private void setAppIcon() {
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/logo_tr.png")));
            Image img = icon.getImage();
            setIconImage(img);
        } catch (Exception e) {
            System.err.println(conf.getTranslation("iconNotFound") + ": " + e.getMessage());
        }
    }
    private void logCurrentDirectory(String action) {
        String currentDir = System.getProperty("user.dir");
        FileUtils.appendToConsole(consoleArea, action + ": " + conf.getTranslation("currentDir") + ": " + currentDir);
    }

    private void refreshFilesList(JList<String> list, String folderName) {
        File folder = new File(folderName);
        System.out.println(conf.getTranslation("checkingFolderExists") + ": " + folder.getAbsolutePath());
        if (folder.exists() && folder.isDirectory()) {
            String[] files = folder.list((dir, name) -> name.endsWith(".jar"));
            if (files != null) {
                list.setListData(files);
            }
        } else {
            System.out.println(conf.getTranslation("folderNotFound"));
        }
    }

    private void clearFilesList(JList<String> list) {
        list.setListData(new String[0]);
    }
    private void clearConsole() {
        consoleArea.setText("");
    }
    private boolean confirmAction(String message) {
        int option = JOptionPane.showConfirmDialog(this, message, conf.getTranslation("confirmActionTitle"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return option == JOptionPane.YES_OPTION;
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LanguageSelector.showLanguageSelector(() -> {
                SwingUtilities.invokeLater(() -> {
                    Conf conf = new Conf();
                    new MainWindow(conf).setVisible(true);
                });
            });
        });
    }
}