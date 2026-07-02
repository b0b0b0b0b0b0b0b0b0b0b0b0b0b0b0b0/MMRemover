package bm.b0b0b0.util.inspector.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class InspectionReport {

    public File jarFile;
    public String jarName = "";
    public long jarSize;
    public int totalEntries;
    public int classCount;
    public int shortNameClasses;

    public final Map<UrlBucket, Map<String, Set<String>>> urls = new EnumMap<>(UrlBucket.class);
    public final Map<String, Set<String>> discordWebhooks = new TreeMap<>();
    public final Map<String, Set<String>> telegramBots = new TreeMap<>();
    public final Map<String, Set<String>> slackWebhooks = new TreeMap<>();
    public final Map<String, Set<String>> rawIps = new TreeMap<>();
    public final Map<String, Set<String>> runtimeUses = new TreeMap<>();

    public final Set<String> seenUrls = new HashSet<>();
    public final List<String> errors = new ArrayList<>();

    public String pluginDescriptorName;
    public String pluginDescriptor;

    public boolean malwareDetected;
    public String malwareReason;
    public final Set<String> malwareRoots = new TreeSet<>();

    public boolean hasThreats() {
        return malwareDetected
                || !urls.getOrDefault(UrlBucket.MALICIOUS, Collections.emptyMap()).isEmpty()
                || !discordWebhooks.isEmpty()
                || !telegramBots.isEmpty()
                || !slackWebhooks.isEmpty();
    }

    public boolean hasUnknowns() {
        return !urls.getOrDefault(UrlBucket.UNKNOWN, Collections.emptyMap()).isEmpty();
    }

    public int suspicionLevel() {
        if (hasThreats()) {
            return 2;
        }
        if (hasUnknowns()) {
            return 1;
        }
        return 0;
    }
}
