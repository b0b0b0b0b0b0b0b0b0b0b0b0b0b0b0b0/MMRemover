package bm.b0b0b0.util.inspector.ui;

import bm.b0b0b0.util.inspector.model.BatchReport;
import bm.b0b0b0.util.inspector.model.InspectionReport;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public final class InspectorNavRenderer extends DefaultListCellRenderer {

    private final BatchReport batch;
    private final String overviewLabel;

    public InspectorNavRenderer(BatchReport batch, String overviewLabel) {
        this.batch = batch;
        this.overviewLabel = overviewLabel;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 10, 4, 10));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setBackground(isSelected ? InspectorTheme.LINE : InspectorTheme.CARD);
        label.setForeground(InspectorTheme.FOREGROUND);

        if (index == 0) {
            label.setText(overviewLabel);
            label.setFont(new Font("Segoe UI", Font.BOLD, 13));
            return label;
        }

        InspectionReport report = batch.reports.get(index - 1);
        label.setText(report.jarName + "  " + ReportRenderer.badge(report));
        label.setForeground(colorFor(report));
        return label;
    }

    private static Color colorFor(InspectionReport report) {
        if (report.hasThreats()) {
            return InspectorTheme.THREAT;
        }
        if (report.hasUnknowns()) {
            return InspectorTheme.WARNING;
        }
        return InspectorTheme.CLEAN;
    }
}
