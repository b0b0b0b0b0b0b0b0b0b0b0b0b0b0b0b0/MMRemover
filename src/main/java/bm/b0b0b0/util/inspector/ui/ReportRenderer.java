package bm.b0b0b0.util.inspector.ui;

import bm.b0b0b0.util.gui.Conf;
import bm.b0b0b0.util.inspector.ioc.UrlClassifier;
import bm.b0b0b0.util.inspector.model.BatchReport;
import bm.b0b0b0.util.inspector.model.InspectionReport;
import bm.b0b0b0.util.inspector.model.UrlBucket;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ReportRenderer {

    private final Conf conf;

    public ReportRenderer(Conf conf) {
        this.conf = conf;
    }

    public String renderOverview(BatchReport batch) {
        StringBuilder builder = new StringBuilder(1024);
        int total = batch.reports.size();
        long suspicious = batch.reports.stream().filter(InspectionReport::hasUnknowns).count();

        if (batch.threatCount == 0 && suspicious == 0) {
            builder.append(String.format(conf.getTranslation("inspOverviewAllClean"), total));
        } else {
            builder.append(String.format(conf.getTranslation("inspOverviewLine"),
                    total, batch.threatCount, batch.cleanCount));
            if (suspicious > 0) {
                builder.append('\n')
                        .append(String.format(conf.getTranslation("inspOverviewSuspicious"), suspicious));
            }
        }
        builder.append("\n\n");

        for (InspectionReport report : batch.reports) {
            appendPluginCard(builder, report, true);
            builder.append('\n');
        }

        return builder.toString().stripTrailing() + '\n';
    }

    public String renderPlugin(InspectionReport report) {
        StringBuilder builder = new StringBuilder(4096);
        builder.append(report.jarName);
        if (report.hasThreats()) {
            builder.append("  ").append(badge(report));
        } else if (report.hasUnknowns()) {
            builder.append("  ").append(badge(report));
        }
        builder.append('\n');
        builder.append(String.format(conf.getTranslation("inspMetaLine"),
                formatSize(report.jarSize), report.classCount, report.totalEntries));
        builder.append("\n\n");

        appendFindings(builder, report, false);

        Map<String, Set<String>> safe = report.urls.getOrDefault(UrlBucket.SAFE, Collections.emptyMap());
        if (!safe.isEmpty()) {
            builder.append('\n');
            appendBlock(builder, conf.getTranslation("inspLabelSafe"),
                    () -> appendSafeHosts(builder, safe));
        }

        if (report.pluginDescriptor != null) {
            builder.append('\n');
            appendBlock(builder, conf.getTranslation("inspLabelDescriptor"),
                    () -> appendDescriptor(builder, report));
        }

        if (!report.runtimeUses.isEmpty()) {
            builder.append('\n');
            appendBlock(builder, conf.getTranslation("inspLabelRuntime"),
                    () -> appendRuntime(builder, report));
        }

        if (report.classCount > 0) {
            int percent = (int) Math.round(100.0 * report.shortNameClasses / Math.max(1, report.classCount));
            if (percent >= 25) {
                builder.append('\n');
                builder.append(String.format(conf.getTranslation("inspObfLine"), percent));
            }
        }

        if (!report.errors.isEmpty()) {
            builder.append('\n');
            appendBlock(builder, conf.getTranslation("inspLabelErrors"),
                    () -> {
                        for (String error : report.errors) {
                            builder.append("  • ").append(error).append('\n');
                        }
                    });
        }

        return builder.toString().stripTrailing() + '\n';
    }

    public static String badge(InspectionReport report) {
        if (report.hasThreats()) {
            return "[!]";
        }
        if (report.hasUnknowns()) {
            return "[?]";
        }
        return "[+]";
    }

    private void appendPluginCard(StringBuilder builder, InspectionReport report, boolean compact) {
        builder.append(badge(report)).append(' ').append(report.jarName).append('\n');
        appendFindings(builder, report, compact);
    }

    private void appendFindings(StringBuilder builder, InspectionReport report, boolean compact) {
        boolean any = false;

        if (report.malwareDetected) {
            any = true;
            builder.append("  [!] ").append(report.malwareReason).append('\n');
            for (String root : report.malwareRoots) {
                builder.append("      ").append(root).append('\n');
            }
        }

        any |= appendUrlBlock(builder, report.urls.getOrDefault(UrlBucket.MALICIOUS, Collections.emptyMap()),
                conf.getTranslation("inspLabelBadUrls"), compact);
        any |= appendWebhookBlock(builder, report, compact);
        any |= appendIpBlock(builder, report.rawIps, compact);
        any |= appendUrlBlock(builder, report.urls.getOrDefault(UrlBucket.UNKNOWN, Collections.emptyMap()),
                conf.getTranslation("inspLabelUnknown"), compact);

        if (!any) {
            builder.append("  ").append(conf.getTranslation("inspPluginOk")).append('\n');
        }
    }

    private boolean appendUrlBlock(StringBuilder builder,
                                   Map<String, Set<String>> urls,
                                   String label,
                                   boolean compact) {
        if (urls.isEmpty()) {
            return false;
        }
        builder.append("  ").append(label).append('\n');
        for (Map.Entry<String, Set<String>> entry : urls.entrySet()) {
            builder.append("  • ").append(entry.getKey());
            appendSourcesInline(builder, entry.getValue(), compact);
            builder.append('\n');
        }
        return true;
    }

    private boolean appendWebhookBlock(StringBuilder builder, InspectionReport report, boolean compact) {
        boolean any = false;
        any |= appendSimpleUrls(builder, conf.getTranslation("inspLabelDiscord"),
                report.discordWebhooks, compact);
        any |= appendSimpleUrls(builder, conf.getTranslation("inspLabelTelegram"),
                report.telegramBots, compact);
        any |= appendSimpleUrls(builder, conf.getTranslation("inspLabelSlack"),
                report.slackWebhooks, compact);
        return any;
    }

    private boolean appendSimpleUrls(StringBuilder builder,
                                     String label,
                                     Map<String, Set<String>> urls,
                                     boolean compact) {
        if (urls.isEmpty()) {
            return false;
        }
        builder.append("  ").append(label).append('\n');
        for (Map.Entry<String, Set<String>> entry : urls.entrySet()) {
            builder.append("  • ").append(entry.getKey());
            appendSourcesInline(builder, entry.getValue(), compact);
            builder.append('\n');
        }
        return true;
    }

    private boolean appendIpBlock(StringBuilder builder, Map<String, Set<String>> ips, boolean compact) {
        if (ips.isEmpty()) {
            return false;
        }
        builder.append("  ").append(conf.getTranslation("inspLabelIps")).append('\n');
        for (Map.Entry<String, Set<String>> entry : ips.entrySet()) {
            builder.append("  • ").append(entry.getKey());
            appendSourcesInline(builder, entry.getValue(), compact);
            builder.append('\n');
        }
        return true;
    }

    private void appendSourcesInline(StringBuilder builder, Set<String> sources, boolean compact) {
        if (sources.isEmpty()) {
            return;
        }
        if (compact) {
            builder.append(String.format(conf.getTranslation("inspUrlSource"), firstSource(sources)));
            return;
        }
        for (String source : sources) {
            builder.append(String.format(conf.getTranslation("inspUrlSource"), source));
        }
    }

    private void appendDescriptor(StringBuilder builder, InspectionReport report) {
        builder.append("  [").append(report.pluginDescriptorName).append("]\n");
        String descriptor = report.pluginDescriptor;
        if (descriptor.length() > 2000) {
            descriptor = descriptor.substring(0, 2000) + " " + conf.getTranslation("inspTruncated");
        }
        for (String line : descriptor.split("\n", -1)) {
            builder.append("  ").append(line).append('\n');
        }
    }

    private void appendSafeHosts(StringBuilder builder, Map<String, Set<String>> safe) {
        builder.append(String.format(conf.getTranslation("inspSafeCount"), safe.size())).append('\n');
        for (Map.Entry<String, Set<String>> entry : safe.entrySet()) {
            builder.append("  • ").append(entry.getKey());
            for (String source : entry.getValue()) {
                builder.append(String.format(conf.getTranslation("inspUrlSource"), source));
            }
            builder.append('\n');
        }
    }

    private void appendRuntime(StringBuilder builder, InspectionReport report) {
        String classesFormat = conf.getTranslation("inspClassesSuffix");
        for (Map.Entry<String, Set<String>> entry : report.runtimeUses.entrySet()) {
            builder.append("  • ").append(shortRuntimeMarker(entry.getKey()))
                    .append(" — ")
                    .append(String.format(classesFormat, entry.getValue().size()))
                    .append('\n');
        }
    }

    private void appendBlock(StringBuilder builder, String label, Runnable body) {
        builder.append(label).append('\n');
        body.run();
    }

    private static String firstSource(Set<String> sources) {
        return sources.iterator().next();
    }

    private static String shortRuntimeMarker(String marker) {
        return marker
                .replace("java/lang/", "")
                .replace("java/net/", "");
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
