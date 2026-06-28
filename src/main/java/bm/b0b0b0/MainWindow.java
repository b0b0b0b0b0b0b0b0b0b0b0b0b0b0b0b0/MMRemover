package bm.b0b0b0;

import bm.b0b0b0.util.AppLinks;
import bm.b0b0b0.util.gui.Conf;
import bm.b0b0b0.util.gui.DragAndDropHandler;
import bm.b0b0b0.util.gui.StyledButton;
import bm.b0b0b0.util.files.FileListCellRenderer;
import bm.b0b0b0.util.files.FileListInteraction;
import bm.b0b0b0.util.files.FileProcessor;
import bm.b0b0b0.util.files.FileUtils;
import bm.b0b0b0.util.gui.UIManagerUtil;
import bm.b0b0b0.util.gui.LanguageSwitcher;
import bm.b0b0b0.util.gui.load.LoadingDialog;
import bm.b0b0b0.util.update.AppVersion;
import bm.b0b0b0.util.update.UpdateChecker;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
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
        setSize(800, 600);
        setMinimumSize(new Dimension(600, 400));
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
                UIManagerUtil.getTrashIcon(),
                conf.getTranslation("clearInputListButton")
        );
        JPanel inputHeader = UIManagerUtil.createPanelHeader(
                conf.getTranslation("inputPanelTitle"), openInputFolderButton, clearInputListButton);
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
                UIManagerUtil.getTrashIcon(),
                conf.getTranslation("clearOutputListButton")
        );
        JPanel outputHeader = UIManagerUtil.createPanelHeader(
                conf.getTranslation("outputPanelTitle"), openOutputFolderButton, clearOutputListButton);
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
        FileListInteraction.enableFileDelete(inputList, consoleArea, conf,
                () -> refreshFilesList(inputList, pluginsDir.toString()));
        FileListInteraction.enableFileDelete(outputList, consoleArea, conf,
                () -> refreshFilesList(outputList, "out"));
        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        JPanel consolePanel = new JPanel(new BorderLayout(0, 2));
        consolePanel.setBackground(COLOR_MAIN_BG);
        JButton copyConsoleButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getCopyIcon(),
                conf.getTranslation("copyConsoleButton")
        );
        JButton clearConsoleButton = UIManagerUtil.createIconButton(
                UIManagerUtil.getTrashIcon(),
                conf.getTranslation("clearConsoleButton")
        );
        JPanel consoleHeader = UIManagerUtil.createPanelHeader(
                conf.getTranslation("consolePanelTitle"), copyConsoleButton, clearConsoleButton);
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

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    logCurrentDirectory(conf.getTranslation("cleanupAllStart"));
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "hostflow", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "artemka", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "bstatsjar", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "pluginmetrics", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "aph", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "chbkHack", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "ruBstatsHack", consoleArea, conf);
                    FileProcessor.processFiles(new File(pluginsDir.toString()).toPath(), new File("out").toPath(), "systemMetricsHack", consoleArea, conf);
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

        copyConsoleButton.addActionListener(e -> copyConsole());

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
            System.out.println(conf.getTranslation("folderNotFound"));
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