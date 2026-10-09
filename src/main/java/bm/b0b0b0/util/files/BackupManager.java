package bm.b0b0b0.util.files;

import bm.b0b0b0.util.gui.Conf;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BackupManager {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int KEEP_BACKUPS = 5;

    private BackupManager() {
    }

    public static Path backupsDir() {
        return Paths.get("backups");
    }

    public static Path createBackup(Path sourceDir, Conf conf, Consumer<String> progress) throws IOException {
        if (sourceDir == null || !Files.isDirectory(sourceDir)) {
            throw new IOException(String.format(conf.getTranslation("backupSourceMissing"),
                    sourceDir == null ? "-" : sourceDir.toString()));
        }

        Path dir = backupsDir();
        Files.createDirectories(dir);

        String base = sourceDir.getFileName() == null ? "backup" : sourceDir.getFileName().toString();
        Path target = dir.resolve(base + "-" + LocalDateTime.now().format(STAMP) + ".zip");

        Path tmp = Files.createTempFile(dir, base + "-", ".zip.part");
        try (OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING);
             ZipOutputStream zip = new ZipOutputStream(os)) {
            try (Stream<Path> walk = Files.walk(sourceDir)) {
                List<Path> files = walk.filter(Files::isRegularFile).toList();
                for (Path file : files) {
                    String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
                    if (progress != null) {
                        progress.accept(entryName);
                    }
                    zip.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zip);
                    zip.closeEntry();
                }
            }
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }

        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        pruneOldBackups(dir);
        return target;
    }

    private static void pruneOldBackups(Path dir) {
        List<Path> zips;
        try (Stream<Path> list = Files.list(dir)) {
            zips = list
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .sorted(Comparator.comparingLong(BackupManager::lastModified).reversed())
                    .toList();
        } catch (IOException e) {
            return;
        }
        for (int i = KEEP_BACKUPS; i < zips.size(); i++) {
            try {
                Files.deleteIfExists(zips.get(i));
            } catch (IOException ignored) {
            }
        }
    }

    private static long lastModified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
