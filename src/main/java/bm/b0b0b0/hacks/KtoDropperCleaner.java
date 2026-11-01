package bm.b0b0b0.hacks;

import bm.b0b0b0.util.files.JarEntryUtils;
import org.objectweb.asm.ClassReader;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class KtoDropperCleaner {

    public record Result(
            boolean infected,
            String error,
            int sites,
            List<String> removed,
            List<String> patched,
            List<JarEntryUtils.Entry> entries
    ) {
    }

    private KtoDropperCleaner() {
    }

    public static boolean infected(List<JarEntryUtils.Entry> entries) {
        return !owners(entries).isEmpty() || hasBlob(entries);
    }

    public static Result clean(List<JarEntryUtils.Entry> entries) {
        Set<String> droppers = owners(entries);
        boolean blob = hasBlob(entries);
        if (droppers.isEmpty() && !blob) {
            return new Result(false, null, 0, List.of(), List.of(), entries);
        }

        List<String> removed = new ArrayList<>();
        List<String> patched = new ArrayList<>();
        List<JarEntryUtils.Entry> kept = new ArrayList<>();
        int sites = 0;

        for (JarEntryUtils.Entry entry : entries) {
            if (KtoDropperMalwareDetector.BLOB_ENTRY.equals(entry.name())
                    || KtoDropperMalwareDetector.isPayloadEntry(entry.name(), droppers)) {
                removed.add(entry.name());
                continue;
            }
            if (!entry.isDirectory()
                    && entry.name().endsWith(".class")
                    && entry.data() != null
                    && !droppers.isEmpty()) {
                KtoDropperMalwareDetector.Patch patch =
                        KtoDropperMalwareDetector.neutralizeCalls(entry.data(), droppers);
                if (patch == null) {
                    return new Result(true,
                            "unsafe call in " + entry.name(),
                            0, List.of(), List.of(), entries);
                }
                if (patch.sites() > 0) {
                    sites += patch.sites();
                    patched.add(entry.name());
                    kept.add(new JarEntryUtils.Entry(entry.name(), false, patch.bytes()));
                    continue;
                }
            }
            kept.add(entry);
        }

        List<JarEntryUtils.Entry> finished = new ArrayList<>();
        for (JarEntryUtils.Entry entry : kept) {
            if (isSignatureEntry(entry.name()) || isEmptyPayloadDir(entry, removed, kept)) {
                removed.add(entry.name());
                continue;
            }
            finished.add(entry);
        }
        return new Result(true, null, sites, List.copyOf(removed), List.copyOf(patched), finished);
    }

    private static Set<String> owners(List<JarEntryUtils.Entry> entries) {
        Set<String> droppers = new LinkedHashSet<>();
        for (JarEntryUtils.Entry entry : entries) {
            if (entry.isDirectory() || entry.data() == null || !entry.name().endsWith(".class")) {
                continue;
            }
            if (!KtoDropperMalwareDetector.isDropperClass(entry.data())) {
                continue;
            }
            try {
                droppers.add(new ClassReader(entry.data()).getClassName());
            } catch (Throwable ignored) {
                String name = entry.name();
                droppers.add(name.substring(0, name.length() - ".class".length()));
            }
        }
        return droppers;
    }

    private static boolean hasBlob(List<JarEntryUtils.Entry> entries) {
        for (JarEntryUtils.Entry entry : entries) {
            if (KtoDropperMalwareDetector.BLOB_ENTRY.equals(entry.name())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSignatureEntry(String name) {
        if (name == null || !name.startsWith("META-INF/")) {
            return false;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC");
    }

    private static boolean isEmptyPayloadDir(JarEntryUtils.Entry entry, List<String> removed, List<JarEntryUtils.Entry> kept) {
        if (!entry.isDirectory() && (entry.data() != null || !entry.name().endsWith("/"))) {
            return false;
        }
        if (!entry.name().endsWith("/")) {
            return false;
        }
        boolean coversRemoved = false;
        for (String name : removed) {
            if (name.startsWith(entry.name())) {
                coversRemoved = true;
                break;
            }
        }
        if (!coversRemoved) {
            return false;
        }
        for (JarEntryUtils.Entry other : kept) {
            if (other == entry || other.name().equals(entry.name())) {
                continue;
            }
            if (other.name().startsWith(entry.name()) && !other.name().endsWith("/")) {
                return false;
            }
        }
        return true;
    }
}
