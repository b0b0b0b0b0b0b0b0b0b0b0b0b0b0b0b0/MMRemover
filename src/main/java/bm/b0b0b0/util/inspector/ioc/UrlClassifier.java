package bm.b0b0b0.util.inspector.ioc;

import bm.b0b0b0.util.inspector.model.UrlBucket;

import java.util.Locale;

public final class UrlClassifier {

    private UrlClassifier() {
    }

    public static UrlBucket classify(String host, String url) {
        if (host == null || host.isEmpty()) {
            return UrlBucket.UNKNOWN;
        }

        String hostLc = host.toLowerCase(Locale.ROOT);
        for (String bad : ThreatCatalog.MALICIOUS_HOSTS) {
            if (hostMatches(hostLc, bad)) {
                return UrlBucket.MALICIOUS;
            }
        }

        if (url != null) {
            String urlLc = url.toLowerCase(Locale.ROOT);
            for (String fragment : ThreatCatalog.MALICIOUS_URL_FRAGMENTS) {
                if (urlLc.contains(fragment)) {
                    return UrlBucket.MALICIOUS;
                }
            }
        }

        for (String safe : ThreatCatalog.SAFE_HOST_MARKERS) {
            if (hostMatches(hostLc, safe)) {
                return UrlBucket.SAFE;
            }
        }

        return UrlBucket.UNKNOWN;
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

    private static boolean hostMatches(String hostLc, String marker) {
        return hostLc.equals(marker) || hostLc.endsWith("." + marker);
    }
}
