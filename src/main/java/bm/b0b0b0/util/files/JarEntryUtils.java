package bm.b0b0b0.util.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class JarEntryUtils {

    public record Entry(String name, boolean isDirectory, byte[] data) {
    }

    public static List<Entry> readDeduped(File jar) throws IOException {
        Map<String, Entry> map = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                Entry candidate = new Entry(
                        e.getName(),
                        e.isDirectory(),
                        e.isDirectory() ? null : zip.getInputStream(e).readAllBytes()
                );
                Entry existing = map.get(e.getName());
                if (existing == null || prefer(candidate, existing)) {
                    map.put(e.getName(), candidate);
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    private static boolean prefer(Entry candidate, Entry existing) {
        if (candidate.isDirectory() != existing.isDirectory()) {
            return !candidate.isDirectory();
        }
        int candidateLen = candidate.data == null ? 0 : candidate.data.length;
        int existingLen = existing.data == null ? 0 : existing.data.length;
        return candidateLen > existingLen;
    }

    public static void writeJar(File outFile, List<Entry> entries) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFile))) {
            for (Entry entry : entries) {
                out.putNextEntry(new ZipEntry(entry.name()));
                if (!entry.isDirectory() && entry.data() != null) {
                    out.write(entry.data());
                }
                out.closeEntry();
            }
        }
    }

    private JarEntryUtils() {
    }
}
