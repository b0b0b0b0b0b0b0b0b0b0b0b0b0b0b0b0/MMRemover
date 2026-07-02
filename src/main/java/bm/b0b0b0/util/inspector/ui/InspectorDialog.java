package bm.b0b0b0.util.inspector.ui;

import bm.b0b0b0.util.gui.Conf;
import bm.b0b0b0.util.inspector.model.BatchReport;
import bm.b0b0b0.util.inspector.model.InspectionReport;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InspectorDialog extends JDialog {

    private static final String OVERVIEW_KEY = "__overview__";

    private final Conf conf;
    private final BatchReport batch;
    private final List<String> pageKeys = new ArrayList<>();
    private final Map<String, String> plainTextByKey = new HashMap<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JList<String> nav;

    public static void show(Frame owner, BatchReport batch, Conf conf) {
        InspectorDialog dialog = new InspectorDialog(owner, batch, conf);
        dialog.setVisible(true);
    }

    private InspectorDialog(Frame owner, BatchReport batch, Conf conf) {
        super(owner, String.format(conf.getTranslation("inspTitleFmt"), batch.reports.size()), true);
        this.conf = conf;
        this.batch = batch;

        setSize(1000, 680);
        setMinimumSize(new Dimension(700, 400));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(InspectorTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        ReportRenderer renderer = new ReportRenderer(conf);

        javax.swing.DefaultListModel<String> navModel = new javax.swing.DefaultListModel<>();
        navModel.addElement(OVERVIEW_KEY);
        pageKeys.add(OVERVIEW_KEY);
        for (InspectionReport report : batch.reports) {
            navModel.addElement(report.jarName);
            pageKeys.add(report.jarName);
        }

        nav = new JList<>(navModel);
        nav.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        nav.setBackground(InspectorTheme.CARD);
        nav.setForeground(InspectorTheme.FOREGROUND);
        nav.setFixedCellHeight(28);
        nav.setCellRenderer(new InspectorNavRenderer(batch, conf.getTranslation("inspNavOverview")));

        JScrollPane navScroll = new JScrollPane(nav);
        navScroll.setBorder(BorderFactory.createLineBorder(InspectorTheme.LINE, 1));
        navScroll.setPreferredSize(new Dimension(260, 0));

        cardPanel.setBackground(InspectorTheme.BACKGROUND);
        addCard(OVERVIEW_KEY, renderer.renderOverview(batch));
        for (InspectionReport report : batch.reports) {
            addCard(report.jarName, renderer.renderPlugin(report));
        }

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navScroll, cardPanel);
        split.setResizeWeight(0.0);
        split.setDividerLocation(260);
        split.setBorder(null);
        split.setBackground(InspectorTheme.BACKGROUND);
        root.add(split, BorderLayout.CENTER);

        JButton prev = styledButton(conf.getTranslation("inspBtnPrev"), InspectorTheme.BUTTON, InspectorTheme.FOREGROUND);
        JButton next = styledButton(conf.getTranslation("inspBtnNext"), InspectorTheme.BUTTON, InspectorTheme.FOREGROUND);
        String copyLabel = conf.getTranslation("inspBtnCopy");
        String copiedLabel = conf.getTranslation("inspBtnCopied");
        JButton copy = styledButton(copyLabel, InspectorTheme.BUTTON, InspectorTheme.FOREGROUND);
        JButton close = styledButton(conf.getTranslation("inspBtnClose"), InspectorTheme.ACCENT, java.awt.Color.WHITE);

        prev.addActionListener(event -> movePage(-1));
        next.addActionListener(event -> movePage(+1));
        copy.addActionListener(event -> {
            String key = pageKeys.get(nav.getSelectedIndex());
            String text = plainTextByKey.getOrDefault(key, "");
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            copy.setText(copiedLabel);
            javax.swing.Timer timer = new javax.swing.Timer(1200, timerEvent -> copy.setText(copyLabel));
            timer.setRepeats(false);
            timer.start();
        });
        close.addActionListener(event -> dispose());

        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        navButtons.setOpaque(false);
        navButtons.add(prev);
        navButtons.add(next);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);
        actionButtons.add(copy);
        actionButtons.add(close);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(navButtons, BorderLayout.WEST);
        bottom.add(actionButtons, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        nav.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int index = nav.getSelectedIndex();
            if (index < 0) {
                return;
            }
            cards.show(cardPanel, pageKeys.get(index));
        });
        nav.setSelectedIndex(0);

        getRootPane().registerKeyboardAction(
                event -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void movePage(int delta) {
        int index = nav.getSelectedIndex();
        int size = pageKeys.size();
        if (size == 0) {
            return;
        }
        int nextIndex = ((index + delta) % size + size) % size;
        nav.setSelectedIndex(nextIndex);
        nav.ensureIndexIsVisible(nextIndex);
    }

    private void addCard(String key, String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBackground(InspectorTheme.CARD);
        area.setForeground(InspectorTheme.FOREGROUND);
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(InspectorTheme.LINE, 1));
        cardPanel.add(scroll, key);
        plainTextByKey.put(key, text);
    }

    private static JButton styledButton(String text, java.awt.Color background, java.awt.Color foreground) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setPreferredSize(new Dimension(150, 32));
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style",
                "arc: 6; focusWidth: 0;"
                        + "background: " + InspectorTheme.toHex(background) + ";"
                        + "foreground: " + InspectorTheme.toHex(foreground) + ";"
                        + "hoverBackground: " + InspectorTheme.toHex(background.brighter()) + ";"
                        + "pressedBackground: " + InspectorTheme.toHex(background.darker()) + ";");
        return button;
    }
}
