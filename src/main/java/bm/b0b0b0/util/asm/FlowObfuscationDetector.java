package bm.b0b0b0.util.asm;

import java.nio.charset.StandardCharsets;

public final class FlowObfuscationDetector {

    private static final String HASH_ERROR = "Error in hash";

    private static final String[] MARKERS = {
            "goxacdytiiluaxqq/",
            "kmfdonpkiqvngpyj/",
            "gztnqhxvzlczgels",
            "cunvvqihiefdnnok",
            "axotfyodeutqtuza/",
            "ylskjyekdq",
            "vhjkxajwvdpbagyr",
    };

    private FlowObfuscationDetector() {
    }

    public static boolean isFlowObfuscated(byte[] clazz) {
        if (clazz == null || clazz.length == 0) {
            return false;
        }
        String s = new String(clazz, StandardCharsets.ISO_8859_1);
        for (String marker : MARKERS) {
            if (s.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    public static boolean requiresGentlePatch(byte[] clazz) {
        if (clazz == null || clazz.length == 0) {
            return false;
        }
        if (isFlowObfuscated(clazz)) {
            return true;
        }
        return new String(clazz, StandardCharsets.ISO_8859_1).contains(HASH_ERROR);
    }
}
