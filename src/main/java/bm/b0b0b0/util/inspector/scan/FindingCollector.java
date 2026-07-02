package bm.b0b0b0.util.inspector.scan;

import bm.b0b0b0.util.inspector.ioc.IpClassifier;
import bm.b0b0b0.util.inspector.ioc.ThreatCatalog;
import bm.b0b0b0.util.inspector.ioc.UrlClassifier;
import bm.b0b0b0.util.inspector.model.InspectionReport;
import bm.b0b0b0.util.inspector.model.UrlBucket;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

public final class FindingCollector {

    private FindingCollector() {
    }

    public static void collectText(String entryName, String text, InspectionReport report) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (!UrlClassifier.looksLikeRegexPattern(text)) {
            harvestUrls(entryName, text, report);
        }
        harvestIps(entryName, text, report);
        harvestWebhooks(entryName, text, report);
    }

    public static void collectRuntimeMarkers(String entryName, byte[] data, InspectionReport report) {
        for (String marker : ThreatCatalog.RUNTIME_MARKERS) {
            if (indexOf(data, marker) != -1) {
                report.runtimeUses
                        .computeIfAbsent(marker, key -> new TreeSet<>())
                        .add(entryName);
            }
        }
    }

    private static void harvestUrls(String entryName, String text, InspectionReport report) {
        Matcher matcher = ThreatCatalog.URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = UrlClassifier.trimUrl(matcher.group());
            if (!UrlClassifier.isPlausibleHarvestedUrl(url, text, matcher.end())) {
                continue;
            }
            if (UrlClassifier.isWebhookUrl(url)) {
                continue;
            }
            String host = UrlClassifier.hostOf(url);
            if (host == null || host.isEmpty()) {
                continue;
            }

            String dedupeKey = url + "\u0001" + host;
            if (!report.seenUrls.add(dedupeKey)) {
                continue;
            }

            UrlBucket bucket = UrlClassifier.classify(host, url);
            report.urls.computeIfAbsent(bucket, key -> new TreeMap<>())
                    .computeIfAbsent(url, key -> new TreeSet<>())
                    .add(entryName);
        }
    }

    private static void harvestIps(String entryName, String text, InspectionReport report) {
        Matcher matcher = ThreatCatalog.RAW_IPV4.matcher(text);
        while (matcher.find()) {
            String ip = matcher.group();
            String ipOnly = ip.contains(":") ? ip.substring(0, ip.indexOf(':')) : ip;
            if (!IpClassifier.shouldHarvest(ipOnly)) {
                continue;
            }
            report.rawIps.computeIfAbsent(ip, key -> new TreeSet<>()).add(entryName);
        }
    }

    private static void harvestWebhooks(String entryName, String text, InspectionReport report) {
        harvestPattern(ThreatCatalog.DISCORD_WEBHOOK, text, entryName, report.discordWebhooks);
        harvestPattern(ThreatCatalog.TELEGRAM_BOT, text, entryName, report.telegramBots);
        harvestPattern(ThreatCatalog.SLACK_WEBHOOK, text, entryName, report.slackWebhooks);
    }

    private static void harvestPattern(java.util.regex.Pattern pattern,
                                       String text,
                                       String entryName,
                                       Map<String, Set<String>> sink) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            sink.computeIfAbsent(UrlClassifier.trimUrl(matcher.group()), key -> new TreeSet<>())
                    .add(entryName);
        }
    }

    private static int indexOf(byte[] haystack, String needle) {
        byte[] bytes = needle.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > haystack.length) {
            return bytes.length == 0 ? 0 : -1;
        }
        outer:
        for (int i = 0; i <= haystack.length - bytes.length; i++) {
            for (int j = 0; j < bytes.length; j++) {
                if (haystack[i + j] != bytes[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
