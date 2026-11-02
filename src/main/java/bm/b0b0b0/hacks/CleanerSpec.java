package bm.b0b0b0.hacks;

import bm.b0b0b0.util.files.CleanResult;
import bm.b0b0b0.util.files.JarEntryUtils;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public record CleanerSpec(
        String report,
        String infectionKey,
        String neutralizedKey,
        String unsafeKey,
        Predicate<List<JarEntryUtils.Entry>> detector,
        Function<List<JarEntryUtils.Entry>, CleanResult> cleaner
) {
}
