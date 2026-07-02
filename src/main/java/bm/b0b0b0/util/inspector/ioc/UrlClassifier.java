package bm.b0b0b0.util.inspector.ioc;

import bm.b0b0b0.util.inspector.model.UrlBucket;

import java.util.Locale;
import java.util.regex.Matcher;

public final class UrlClassifier {

    private UrlClassifier() {
    }

    public static UrlBucket classify(String host, String url) {
        if (host == null || host.isEmpty()) {
            return UrlBucket.UNKNOWN;
        }

        String hostLc = host.toLowerCase(Locale.ROOT);
        String urlLc = url == null ? "" : url.toLowerCase(Locale.ROOT);

        if (url != null && isWebhookUrl(url)) {
            return UrlBucket.MALICIOUS;
        }

        for (String bad : ThreatCatalog.MALICIOUS_HOSTS) {
            if (hostMatches(hostLc, bad)) {
                return UrlBucket.MALICIOUS;
            }
        }

        if (!urlLc.isEmpty()) {
            for (String fragment : ThreatCatalog.MALICIOUS_URL_FRAGMENTS) {
                if (urlLc.contains(fragment)) {
                    return UrlBucket.MALICIOUS;
                }
            }
        }

        if (isDiscordHost(hostLc)) {
            return UrlBucket.UNKNOWN;
        }

        if (isGitHubRawHost(hostLc)) {
            return UrlBucket.UNKNOWN;
        }

        if (isUserContentHost(hostLc)) {
            return UrlBucket.UNKNOWN;
        }

        for (String safe : ThreatCatalog.SAFE_HOST_MARKERS) {
            if (hostMatches(hostLc, safe)) {
                return UrlBucket.SAFE;
            }
        }

        return UrlBucket.UNKNOWN;
    }

    public static boolean isPlausibleHarvestedUrl(String url, String sourceText, int matchEnd) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        if (sourceText != null && matchEnd < sourceText.length() && sourceText.charAt(matchEnd) == '\\') {
            return false;
        }
        if (url.indexOf('\\') >= 0) {
            return false;
        }
        String host = hostOf(url);
        if (host == null || host.isEmpty()) {
            return false;
        }
        if (isIpv4Host(host)) {
            return true;
        }
        return host.contains(".");
    }

    private static boolean isIpv4Host(String host) {
        return host.matches("^(?:\\d{1,3}\\.){3}\\d{1,3}$");
    }

    public static boolean looksLikeRegexPattern(String text) {
        if (text == null || text.length() < 8) {
            return false;
        }
        return text.startsWith("^")
                && text.endsWith("$")
                && (text.contains("\\.") || text.contains("(?:") || text.contains("\\d"));
    }

    public static boolean isWebhookUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return ThreatCatalog.DISCORD_WEBHOOK.matcher(url).find()
                || ThreatCatalog.TELEGRAM_BOT.matcher(url).find()
                || ThreatCatalog.SLACK_WEBHOOK.matcher(url).find();
    }

    public static boolean isDiscordInvite(String url) {
        if (url == null) {
            return false;
        }
        Matcher matcher = ThreatCatalog.DISCORD_INVITE.matcher(url);
        return matcher.find();
    }

    public static String hostOf(String url) {
        try {
            int schemeEnd = url.indexOf("://");
            if (schemeEnd < 0) {
                return null;
            }
            String rest = url.substring(schemeEnd + 3);
            int slash = rest.indexOf('/');
            String hostPort = slash < 0 ? rest : rest.substring(0, slash);
            int at = hostPort.indexOf('@');
            if (at >= 0) {
                hostPort = hostPort.substring(at + 1);
            }
            int colon = hostPort.indexOf(':');
            return (colon < 0 ? hostPort : hostPort.substring(0, colon)).toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String trimUrl(String value) {
        String trimmed = value;
        while (!trimmed.isEmpty()) {
            char last = trimmed.charAt(trimmed.length() - 1);
            if (last == '.' || last == ',' || last == ')' || last == ']' || last == ';'
                    || last == '"' || last == '\'' || last == '>') {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            } else {
                break;
            }
        }
        return trimmed;
    }

    private static boolean isUserContentHost(String hostLc) {
        for (String marker : ThreatCatalog.USER_CONTENT_HOST_MARKERS) {
            if (hostMatches(hostLc, marker)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGitHubRawHost(String hostLc) {
        return hostLc.equals("raw.githubusercontent.com")
                || hostLc.endsWith(".githubusercontent.com")
                || hostLc.equals("codeload.github.com")
                || hostLc.equals("objects.githubusercontent.com");
    }

    private static boolean isDiscordHost(String hostLc) {
        return hostLc.equals("discord.gg")
                || hostLc.equals("discord.com")
                || hostLc.equals("discordapp.com")
                || hostLc.endsWith(".discord.com")
                || hostLc.endsWith(".discordapp.com");
    }

    private static boolean hostMatches(String hostLc, String marker) {
        return hostLc.equals(marker) || hostLc.endsWith("." + marker);
    }
}
