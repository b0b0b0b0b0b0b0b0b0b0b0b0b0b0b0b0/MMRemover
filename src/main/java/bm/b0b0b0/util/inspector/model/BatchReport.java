package bm.b0b0b0.util.inspector.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BatchReport {

    public final List<InspectionReport> reports = new ArrayList<>();
    public final Map<String, Integer> topMaliciousUrls = new LinkedHashMap<>();
    public final Map<String, Integer> topUnknownUrls = new LinkedHashMap<>();
    public final Map<String, Integer> topRawIps = new LinkedHashMap<>();

    public int threatCount;
    public int cleanCount;

    public void aggregate() {
        Map<String, Integer> malicious = new HashMap<>();
        Map<String, Integer> unknown = new HashMap<>();
        Map<String, Integer> ips = new HashMap<>();

        for (InspectionReport report : reports) {
            if (report.hasThreats()) {
                threatCount++;
            } else {
                cleanCount++;
            }
            for (String url : report.urls
                    .getOrDefault(UrlBucket.MALICIOUS, Map.of())
                    .keySet()) {
                malicious.merge(url, 1, Integer::sum);
            }
            for (String url : report.urls
                    .getOrDefault(UrlBucket.UNKNOWN, Map.of())
                    .keySet()) {
                unknown.merge(url, 1, Integer::sum);
            }
            for (String ip : report.rawIps.keySet()) {
                ips.merge(ip, 1, Integer::sum);
            }
        }

        sortInto(malicious, topMaliciousUrls);
        sortInto(unknown, topUnknownUrls);
        sortInto(ips, topRawIps);
    }

    private static void sortInto(Map<String, Integer> source, Map<String, Integer> target) {
        source.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
                })
                .forEach(entry -> target.put(entry.getKey(), entry.getValue()));
    }
}
