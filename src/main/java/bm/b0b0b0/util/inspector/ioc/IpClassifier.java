package bm.b0b0b0.util.inspector.ioc;

import java.util.Locale;

public final class IpClassifier {

    private static final String[] IGNORED_EXACT = {
            "0.0.0.0",
            "3.53.2.0",
    };

    private IpClassifier() {
    }

    public static boolean shouldHarvest(String ip) {
        String bare = stripPort(ip);
        if (bare.isEmpty()) {
            return false;
        }
        if (isIgnoredExact(bare) || isReserved(bare) || isDocumentation(bare) || looksLikeVersionFragment(bare)) {
            return false;
        }
        return isMalicious(bare);
    }

    public static boolean isMalicious(String ip) {
        String bare = stripPort(ip).toLowerCase(Locale.ROOT);
        for (String marker : ThreatCatalog.MALICIOUS_HOSTS) {
            if (bare.equals(marker.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String stripPort(String ip) {
        int colon = ip.indexOf(':');
        return colon < 0 ? ip : ip.substring(0, colon);
    }

    private static boolean isIgnoredExact(String ip) {
        for (String ignored : IGNORED_EXACT) {
            if (ip.equals(ignored)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReserved(String ip) {
        for (String prefix : ThreatCatalog.RESERVED_IP_PREFIXES) {
            if (ip.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDocumentation(String ip) {
        return ip.startsWith("192.0.2.")
                || ip.startsWith("198.51.100.")
                || ip.startsWith("203.0.113.");
    }

    private static boolean looksLikeVersionFragment(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1]);
            int c = Integer.parseInt(parts[2]);
            int d = Integer.parseInt(parts[3]);
            return a <= 9 && b <= 99 && c <= 99 && d == 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
