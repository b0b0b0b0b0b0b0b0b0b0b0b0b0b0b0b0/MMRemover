package bm.b0b0b0.util.files;

import java.util.List;

public record CleanResult(
        boolean infected,
        String error,
        int sites,
        List<String> removed,
        List<String> patched,
        List<JarEntryUtils.Entry> entries
) {

    public static CleanResult notInfected(List<JarEntryUtils.Entry> entries) {
        return new CleanResult(false, null, 0, List.of(), List.of(), entries);
    }

    public static CleanResult unsafe(List<JarEntryUtils.Entry> entries, String reason) {
        return new CleanResult(true, reason, 0, List.of(), List.of(), entries);
    }
}
