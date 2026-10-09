package bm.b0b0b0;

import bm.b0b0b0.util.AppLinks;
import bm.b0b0b0.util.gui.Conf;
import bm.b0b0b0.util.gui.ConfirmDeleteDialog;
import bm.b0b0b0.util.gui.DragAndDropHandler;
import bm.b0b0b0.util.gui.StyledButton;
import bm.b0b0b0.util.files.FileListCellRenderer;
import bm.b0b0b0.util.files.FileListInteraction;
import bm.b0b0b0.util.files.BackupManager;
import bm.b0b0b0.util.files.FileProcessor;
import bm.b0b0b0.util.files.FileUtils;
import bm.b0b0b0.util.gui.UIManagerUtil;
import bm.b0b0b0.util.gui.LanguageSwitcher;
import bm.b0b0b0.util.gui.load.LoadingDialog;
import bm.b0b0b0.util.inspector.Inspector;
import bm.b0b0b0.util.inspector.model.BatchReport;
import bm.b0b0b0.util.inspector.ui.InspectorDialog;
import bm.b0b0b0.util.log.AppLogger;
import bm.b0b0b0.util.update.AppVersion;
import bm.b0b0b0.util.update.UpdateChecker;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainWindow extends JFrame {
    private JList<String> inputList;
    private JList<String> outputList;
    private JLabel inputPanelTitleLabel;
    private JLabel outputPanelTitleLabel;
    private Path pluginsDir = Paths.get("input");
    private JTextArea consoleArea;
    private final Conf conf;

    private static final Color COLOR_MAIN_BG         = Color.decode("#3c3f41");
    private static final Color COLOR_CONSOLE_BG      = Color.BLACK;
    private static final Color COLOR_CONSOLE_FG      = Color.WHITE;
    private static final Color COLOR_PANEL_TITLE_BG  = Color.decode("#3c3f41");
    private static final Color COLOR_LINK_PANEL_BG   = Color.decode("#3c3f41");
    private static final Color COLOR_BUTTON_BG       = Color.decode("#3c3f41");
    private static final Color COLOR_VISIT_LINK_FG   = Color.decode("#09b55d");


    public MainWindow(Conf conf) {
        this.conf = conf;
        initializeUI();
        setVisible(true);
        UpdateChecker.checkForUpdates(conf, this, consoleArea);
    }

    private void initializeUI() {

        setTitle("MMRemover (v" + AppVersion.CURRENT + ")");
        setSize(960, 600);
        setMinimumSize(new Dimension(920, 420));
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
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(COLOR_MAIN_BG);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(COLOR_BUTTON_BG);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
        JButton removeAllButton = StyledButton.createModernButton(
                conf.getTranslation("removeAllButton"),
                UIManagerUtil.getEraserIcon(),
                StyledButton.Style.PRIMARY);
        removeAllButton.setToolTipText(conf.getTranslation("removeAllButtonTooltip"));
        JButton refreshFilesButton = StyledButton.createModernIconButton(
                UIManagerUtil.getRefreshIcon(),
                StyledButton.Style.SECONDARY,
                conf.getTranslation("refreshFilesButton")
        );
        buttonPanel.add(removeAllButton);
        topPanel.add(buttonPanel, BorderLayout.CENTER);

        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        langPanel.setOpaque(false);
        langPanel.setBackground(COLOR_BUTTON_BG);
        langPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 8));
        langPanel.add(LanguageSwitcher.create(conf, this::restartApplication));
        topPanel.add(langPanel, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        JPanel inputPanel = new JPanel(new BorderLayout(0, 2));
        inputPanel.setBackground(COLOR_PANEL_TITLE_BG);
        inputList = new JList<>();
        inputList.putClientProperty(UIManagerUtil.LIST_DIRECTORY_PATH_KEY, pluginsDir.toString());
        inputList.setCellRenderer(new FileListCellRenderer(pluginsDir.toString(), conf));
        UIManagerUtil.enableListItemHover(inputList);
        JScrollPane inputScrollPane = new JScrollPane(inputList);
        JButton openInputFolderButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getFolderIcon(),
                conf.getTranslation("openInputFolderButton")
        );
        JButton clearInputListButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getBinIcon(),
                conf.getTranslation("clearInputListButton")
        );
        JButton inspectInputButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getInspectorIcon(16),
                conf.getTranslation("inspectInputPanelButton")
        );
        JButton openBackupsButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getBackupIcon(),
                conf.getTranslation("openBackupsButton")
        );
        JPanel inputHeader = UIManagerUtil.createPanelHeader(
                conf.getTranslation("inputPanelTitle"),
                inspectInputButton, openBackupsButton,
                openInputFolderButton, clearInputListButton);
        inputPanelTitleLabel = (JLabel) inputHeader.getClientProperty(UIManagerUtil.PANEL_TITLE_LABEL_KEY);
        inputHeader.setBorder(BorderFactory.createEmptyBorder(4, 8, 2, 4));
        inputPanel.add(inputHeader, BorderLayout.NORTH);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        UIManagerUtil.styleContentPanel(inputPanel);
        inputPanel.setPreferredSize(new Dimension(350, 250));
        JPanel outputPanel = new JPanel(new BorderLayout(0, 2));
        outputPanel.setBackground(COLOR_PANEL_TITLE_BG);
        outputList = new JList<>();
        outputList.putClientProperty(UIManagerUtil.LIST_DIRECTORY_PATH_KEY, "out");
        outputList.setCellRenderer(new FileListCellRenderer("out", conf));
        UIManagerUtil.enableListItemHover(outputList);
        JScrollPane outputScrollPane = new JScrollPane(outputList);
        JButton openOutputFolderButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getFolderIcon(),
                conf.getTranslation("openOutputFolderButton")
        );
        JButton clearOutputListButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getBinIcon(),
                conf.getTranslation("clearOutputListButton")
        );
        JButton inspectOutputButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getInspectorIcon(16),
                conf.getTranslation("inspectOutputPanelButton")
        );
        JPanel outputHeader = UIManagerUtil.createPanelHeader(
                conf.getTranslation("outputPanelTitle"),
                inspectOutputButton, openOutputFolderButton, clearOutputListButton);
        outputPanelTitleLabel = (JLabel) outputHeader.getClientProperty(UIManagerUtil.PANEL_TITLE_LABEL_KEY);
        outputHeader.setBorder(BorderFactory.createEmptyBorder(4, 8, 2, 4));
        outputPanel.add(outputHeader, BorderLayout.NORTH);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);
        UIManagerUtil.styleContentPanel(outputPanel);
        outputPanel.setPreferredSize(new Dimension(350, 250));
        JSplitPane fileSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, outputPanel);
        fileSplitPane.setResizeWeight(0.5);
        fileSplitPane.setBackground(COLOR_MAIN_BG);
        consoleArea = new JTextArea(5, 20);
        consoleArea.setBackground(COLOR_CONSOLE_BG);
        consoleArea.setForeground(COLOR_CONSOLE_FG);
        AppLogger.get().bindConsole(consoleArea);
        AppLogger.get().openSession();
        AppLogger.get().log(String.format(conf.getTranslation("sessionStart"), AppVersion.CURRENT));
        AppLogger.get().log(conf.getTranslation("logFileTarget") + AppLogger.get().logFilePathDisplay());
        AppLogger.get().log(conf.getTranslation("workingDirSet") + System.getProperty("user.dir"));
        FileListInteraction.enableFileDelete(inputList, consoleArea, conf,
                () -> refreshFilesList(inputList, pluginsDir.toString()));
        FileListInteraction.enableFileDelete(outputList, consoleArea, conf,
                () -> refreshFilesList(outputList, "out"));
        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        JPanel consolePanel = new JPanel(new BorderLayout(0, 2));
        consolePanel.setBackground(COLOR_MAIN_BG);
        JButton openLogFolderButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getLogsFolderIcon(),
                conf.getTranslation("openLogFolderButtonTooltip")
        );
        JButton openLogFileButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getLogFileIcon(),
                conf.getTranslation("openLogFileButtonTooltip")
        );
        JButton clearLogArchivesButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getClearLogArchivesIcon(),
                conf.getTranslation("clearLogArchivesButtonTooltip")
        );
        JButton copyConsoleButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getCopyIcon(),
                conf.getTranslation("copyConsoleButton")
        );
        JButton clearConsoleButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getTrashIcon(),
                conf.getTranslation("clearConsoleButton")
        );
        JPanel consoleHeader = UIManagerUtil.createPanelHeader(
                conf.getTranslation("consolePanelTitle"),
                openLogFolderButton, openLogFileButton, clearLogArchivesButton,
                copyConsoleButton, clearConsoleButton);
        consoleHeader.setBorder(BorderFactory.createEmptyBorder(4, 8, 2, 4));
        consolePanel.add(consoleHeader, BorderLayout.NORTH);
        consolePanel.add(consoleScrollPane, BorderLayout.CENTER);
        UIManagerUtil.styleContentPanel(consolePanel);
        consoleScrollPane.setMinimumSize(new Dimension(0, 0));
        consolePanel.setMinimumSize(new Dimension(0, 120));
        consolePanel.setPreferredSize(new Dimension(0, 180));
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, fileSplitPane, consolePanel);
        mainSplitPane.setResizeWeight(0.0);
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(COLOR_MAIN_BG);

        JPanel linkPanel = new JPanel(new BorderLayout());
        linkPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        linkPanel.setBackground(COLOR_LINK_PANEL_BG);

        JButton visitWebsiteButton = new JButton();
        visitWebsiteButton.setToolTipText(conf.getTranslation("pageTitle"));
        visitWebsiteButton.setBorderPainted(false);
        visitWebsiteButton.setOpaque(false);
        visitWebsiteButton.setBackground(null);
        visitWebsiteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        visitWebsiteButton.setFocusPainted(false);

        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/bm.png")));
            Image img = icon.getImage();
            Image scaledImg = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            visitWebsiteButton.setIcon(new ImageIcon(scaledImg));
        } catch (Exception e) {
            System.err.println(conf.getTranslation("Icon_not_found") + ": " + e.getMessage());
        }

        JButton botCheckButton = new JButton(conf.getTranslation("bot_bm"));
        botCheckButton.setToolTipText(conf.getTranslation("bot_bmTooltip"));
        botCheckButton.setForeground(COLOR_VISIT_LINK_FG);
        botCheckButton.setBorderPainted(false);
        botCheckButton.setOpaque(false);
        botCheckButton.setBackground(null);
        botCheckButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botCheckButton.setFocusPainted(false);

        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/bot.png")));
            Image img = icon.getImage();
            Image scaledImg = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            botCheckButton.setIcon(new ImageIcon(scaledImg));
            botCheckButton.setIconTextGap(6);
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

        visitWebsiteButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(AppLinks.PROGRAM_PAGE));
            } catch (IOException | URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        JPanel linksBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        linksBox.setOpaque(false);
        linksBox.add(botCheckButton);
        linksBox.add(visitWebsiteButton);

        linkPanel.add(linksBox, BorderLayout.EAST);
        bottomPanel.add(linkPanel, BorderLayout.CENTER);
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

            SwingWorker<Void, String> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    publish(conf.getTranslation("backupLoadingTitle"));
                    AppLogger.get().info(String.format(conf.getTranslation("backupStart"), pluginsDir.toAbsolutePath()));
                    try {
                        Path backup = BackupManager.createBackup(pluginsDir, conf, null);
                        AppLogger.get().info(String.format(conf.getTranslation("backupDone"), backup.toAbsolutePath()));
                    } catch (IOException ex) {
                        AppLogger.get().error(String.format(conf.getTranslation("backupFailed"), ex.getMessage()));
                    }
                    publish(conf.getTranslation("loadingText"));
                    logCurrentDirectory(conf.getTranslation("cleanupAllStart"));
                    FileProcessor.processAllFiles(pluginsDir, Paths.get("out"), consoleArea, conf);
                    return null;
                }

                @Override
                protected void process(List<String> chunks) {
                    if (!chunks.isEmpty()) {
                        loadingDialog.setStatus(chunks.get(chunks.size() - 1));
                    }
                }

                @Override
                protected void done() {
                    long elapsed = System.currentTimeMillis() - startTime;
                    long minDuration = 3500;

                    if (elapsed < minDuration) {
                        long remain = minDuration - elapsed;
                        Timer t = new Timer((int) remain, actionEvent -> {
                            loadingDialog.dispose();
                            refreshFilesList(inputList, pluginsDir.toString());
                            refreshFilesList(outputList, "out");
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        loadingDialog.dispose();
                        refreshFilesList(inputList, pluginsDir.toString());
                        refreshFilesList(outputList, "out");
                    }
                }
            };

            worker.execute();
            loadingDialog.setVisible(true);
        });

        inspectInputButton.addActionListener(e -> runInspector(pluginsDir, inputList));
        inspectOutputButton.addActionListener(e -> runInspector(Paths.get("out"), outputList));

        JButton chooseFolderButton = StyledButton.createModernButton(
                conf.getTranslation("chooseFolderButton"),
                UIManagerUtil.getFolderIcon(),
                StyledButton.Style.SECONDARY);
        chooseFolderButton.setToolTipText(conf.getTranslation("chooseFolderButtonTooltip"));
        buttonPanel.add(chooseFolderButton);
        buttonPanel.add(refreshFilesButton);
        chooseFolderButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pluginsDir = fc.getSelectedFile().toPath();
                logCurrentDirectory(String.format(conf.getTranslation("folderSelected"), pluginsDir));
                inputList.putClientProperty(UIManagerUtil.LIST_DIRECTORY_PATH_KEY, pluginsDir.toString());
                inputList.setCellRenderer(new FileListCellRenderer(pluginsDir.toString(), conf));
                refreshFilesList(inputList, pluginsDir.toString());
                DragAndDropHandler.addDragAndDropSupport(inputList, consoleArea, pluginsDir.toString(), this::refreshFilesList, conf);
            }
        });


        openInputFolderButton.addActionListener(e -> {
            logCurrentDirectory(conf.getTranslation("openInputFolder"));
            FileUtils.openFolder(pluginsDir.toString(), consoleArea, conf);
        });

        openBackupsButton.addActionListener(e -> {
            try {
                Files.createDirectories(BackupManager.backupsDir());
            } catch (IOException ignored) {
            }
            FileUtils.openFolder(BackupManager.backupsDir().toString(), consoleArea, conf);
        });

        openOutputFolderButton.addActionListener(e -> {
            logCurrentDirectory(conf.getTranslation("openOutputFolder"));
            FileUtils.openFolder("out", consoleArea, conf);
        });

        clearInputListButton.addActionListener(e -> {
            logCurrentDirectory(conf.getTranslation("clearInputList"));
            if (confirmDangerousFolderDelete(pluginsDir.toAbsolutePath().toString())) {
                clearFilesList(inputList);
                FileUtils.clearFolder(new File(pluginsDir.toString()).toPath(), consoleArea, conf);
            }
        });

        clearOutputListButton.addActionListener(e -> {
            logCurrentDirectory(conf.getTranslation("clearOutputList"));
            if (confirmDangerousFolderDelete(new File("out").getAbsolutePath())) {
                clearFilesList(outputList);
                FileUtils.clearFolder(new File("out").toPath(), consoleArea, conf);
            }
        });

        copyConsoleButton.addActionListener(e -> copyConsole());

        openLogFolderButton.addActionListener(e ->
                FileUtils.openFolder(AppLogger.get().logDirectory().toString(), consoleArea, conf));

        openLogFileButton.addActionListener(e ->
                FileUtils.openFile(AppLogger.get().currentLogFile().toFile(), conf));

        clearLogArchivesButton.addActionListener(e -> {
            if (!confirmAction(conf.getTranslation("confirmClearLogArchives"))) {
                return;
            }
            int deleted = AppLogger.get().clearArchives();
            if (deleted < 0) {
                AppLogger.get().error(conf.getTranslation("logArchivesClearFailed"));
            } else if (deleted == 0) {
                AppLogger.get().info(conf.getTranslation("logArchivesNone"));
            } else {
                AppLogger.get().info(String.format(conf.getTranslation("logArchivesCleared"), deleted));
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
        SwingUtilities.invokeLater(() -> {
            mainSplitPane.setDividerLocation(0.68);
            fileSplitPane.setDividerLocation(0.5);
        });

        setVisible(true);
    }


    private void runInspector(Path folder, JList<String> fileList) {
        List<File> jars = resolveJarsToInspect(folder, fileList);
        if (jars.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    String.format(conf.getTranslation("inspectNoJars"), folder.toAbsolutePath()),
                    conf.getTranslation("inspectDialogTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        runInspector(jars);
    }

    private List<File> resolveJarsToInspect(Path folder, JList<String> fileList) {
        File dir = folder.toFile();
        if (!dir.isDirectory()) {
            JOptionPane.showMessageDialog(
                    this,
                    String.format(conf.getTranslation("inspectNoDir"), dir.getAbsolutePath()),
                    conf.getTranslation("inspectDialogTitle"),
                    JOptionPane.WARNING_MESSAGE);
            return List.of();
        }

        String selected = fileList.getSelectedValue();
        if (selected != null && selected.toLowerCase().endsWith(".jar")) {
            File jar = folder.resolve(selected).toFile();
            if (jar.isFile()) {
                return List.of(jar);
            }
        }

        File[] jarFiles = dir.listFiles((unused, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            return List.of();
        }

        Arrays.sort(jarFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return new ArrayList<>(Arrays.asList(jarFiles));
    }

    private void runInspector(List<File> jars) {
        LoadingDialog loadingDialog = new LoadingDialog(this, conf, "inspectLoadingTitle");
        SwingWorker<BatchReport, String> worker = new SwingWorker<>() {
            @Override
            protected BatchReport doInBackground() {
                return Inspector.inspectAll(jars, this::publish);
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String latest = chunks.get(chunks.size() - 1);
                    loadingDialog.setStatus(String.format(conf.getTranslation("inspectProgressFmt"), latest));
                }
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    BatchReport report = get();
                    InspectorDialog.show(MainWindow.this, report, conf);
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(
                            MainWindow.this,
                            String.format(conf.getTranslation("inspectFailed"), exception.getMessage()),
                            conf.getTranslation("inspectDialogTitle"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
        loadingDialog.setVisible(true);
    }

    private void setWorkingDirectoryToJarLocation() {
        try {
            File location = new File(MainWindow.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File base = location.isDirectory()
                    ? location.getParentFile().getParentFile()
                    : location.getParentFile();
            if (!new File(base, "input").isDirectory()) {
                File parent = base.getParentFile();
                if (parent != null && new File(parent, "input").isDirectory()) {
                    base = parent;
                }
            }
            System.setProperty("user.dir", base.getAbsolutePath());
            pluginsDir = Paths.get(base.getAbsolutePath(), "input");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            AppLogger.get().error(conf.getTranslation("workingDirFailed"));
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
        AppLogger.get().log(conf.getTranslation("checkingFolderExists") + ": " + folder.getAbsolutePath());
        int count = 0;
        if (folder.exists() && folder.isDirectory()) {
            String[] files = folder.list((dir, name) -> name.endsWith(".jar"));
            if (files != null) {
                count = files.length;
                list.setListData(files);
            } else {
                list.setListData(new String[0]);
            }
        } else {
            AppLogger.get().log(conf.getTranslation("folderNotFound"));
            list.setListData(new String[0]);
        }
        updatePanelTitle(list, count);
    }

    private void updatePanelTitle(JList<String> list, int count) {
        JLabel titleLabel;
        String titleKey;
        if (list == inputList) {
            titleLabel = inputPanelTitleLabel;
            titleKey = "inputPanelTitle";
        } else if (list == outputList) {
            titleLabel = outputPanelTitleLabel;
            titleKey = "outputPanelTitle";
        } else {
            return;
        }
        if (titleLabel != null) {
            titleLabel.setText(String.format("%s (%d)", conf.getTranslation(titleKey), count));
        }
    }

    private void clearFilesList(JList<String> list) {
        list.setListData(new String[0]);
        updatePanelTitle(list, 0);
    }
    private void copyConsole() {
        String text = consoleArea.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    private void clearConsole() {
        consoleArea.setText("");
    }
    private boolean confirmAction(String message) {
        Object[] options = {
                conf.getTranslation("confirmYes"),
                conf.getTranslation("confirmNo")
        };
        int option = JOptionPane.showOptionDialog(
                this,
                message,
                conf.getTranslation("confirmActionTitle"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
        );
        return option == JOptionPane.YES_OPTION;
    }

    private boolean confirmDangerousFolderDelete(String folderDisplay) {
        int[] countdownSeconds = {5, 4, 3};
        String[] stageKeys = {"dangerDeleteStage1", "dangerDeleteStage2", "dangerDeleteStage3"};
        for (int stage = 0; stage < stageKeys.length; stage++) {
            String message = String.format(conf.getTranslation(stageKeys[stage]), folderDisplay);
            if (!ConfirmDeleteDialog.show(this, conf, message, countdownSeconds[stage])) {
                return false;
            }
        }
        return true;
    }

    private void restartApplication() {
        SwingUtilities.invokeLater(() -> {
            dispose();
            new MainWindow(conf).setVisible(true);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Conf conf = new Conf();
            new MainWindow(conf).setVisible(true);
        });
    }
}