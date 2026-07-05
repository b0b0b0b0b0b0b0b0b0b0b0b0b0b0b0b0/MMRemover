package bm.b0b0b0.util.log;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class AppLogger {

    public enum Level { INFO, WARN, ERROR }

    private static final AppLogger INSTANCE = new AppLogger();
    private static final Path LOG_DIR = Path.of("logs");
    private static final String LATEST_LOG = "latest.log";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter UI_TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Object lock = new Object();
    private volatile JTextArea console;
    private BufferedWriter writer;

    public static AppLogger get() {
        return INSTANCE;
    }

    public void bindConsole(JTextArea area) {
        console = area;
    }

    public void openSession() {
        synchronized (lock) {
            try {
                Files.createDirectories(LOG_DIR);
                rotateLatestIfExists();
                openLatestFile();
                writer.flush();
            } catch (IOException e) {
                System.err.println("Log init failed: " + e.getMessage());
            }
        }
    }

    public String logFilePathDisplay() {
        synchronized (lock) {
            return currentLogPath().toAbsolutePath().toString().replace('\\', '/');
        }
    }

    public Path logDirectory() {
        synchronized (lock) {
            try {
                Files.createDirectories(LOG_DIR);
            } catch (IOException ignored) {
            }
            return LOG_DIR.toAbsolutePath();
        }
    }

    public Path currentLogFile() {
        synchronized (lock) {
            return currentLogPath().toAbsolutePath();
        }
    }

    public int clearArchives() {
        synchronized (lock) {
            try {
                Files.createDirectories(LOG_DIR);
                int deleted = 0;
                try (var stream = Files.list(LOG_DIR)) {
                    for (Path path : stream.toList()) {
                        if (!Files.isRegularFile(path)) {
                            continue;
                        }
                        if (LATEST_LOG.equalsIgnoreCase(path.getFileName().toString())) {
                            continue;
                        }
                        Files.delete(path);
                        deleted++;
                    }
                }
                return deleted;
            } catch (IOException e) {
                return -1;
            }
        }
    }

    public void log(String message) {
        info(message);
    }

    public void info(String message) {
        emit(Level.INFO, message);
    }

    public void warn(String message) {
        emit(Level.WARN, message);
    }

    public void error(String message) {
        emit(Level.ERROR, message);
    }

    private void emit(Level level, String message) {
        String uiLine = uiLine(message);
        String fileLine = fileLine(level, message);
        writeFile(fileLine);
        mirror(level, uiLine);
        appendUi(uiLine);
    }

    private static String uiLine(String message) {
        return "[" + LocalTime.now().format(UI_TS) + "] " + message;
    }

    private static String fileLine(Level level, String message) {
        return "[" + LocalTime.now().format(UI_TS) + " " + level.name() + "]: " + message;
    }

    private void writeFile(String line) {
        synchronized (lock) {
            if (!ensureWriter()) {
                return;
            }
            try {
                writeRaw(line);
                writer.flush();
            } catch (IOException ignored) {
            }
        }
    }

    private void writeRaw(String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }

    private boolean ensureWriter() {
        try {
            if (writer != null) {
                return true;
            }
            Files.createDirectories(LOG_DIR);
            openLatestFile();
            return writer != null;
        } catch (IOException e) {
            return false;
        }
    }

    private void rotateLatestIfExists() throws IOException {
        Path latest = currentLogPath();
        if (!Files.isRegularFile(latest) || Files.size(latest) == 0) {
            return;
        }
        Path archive = nextArchivePath();
        Files.move(latest, archive, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path nextArchivePath() throws IOException {
        String date = LocalDate.now().format(DAY);
        int index = 1;
        Path candidate;
        do {
            candidate = LOG_DIR.resolve(date + "-" + index + ".log");
            index++;
        } while (Files.exists(candidate));
        return candidate;
    }

    private void openLatestFile() throws IOException {
        closeWriter();
        writer = Files.newBufferedWriter(
                currentLogPath(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private static Path currentLogPath() {
        return LOG_DIR.resolve(LATEST_LOG);
    }

    private void closeWriter() {
        if (writer == null) {
            return;
        }
        try {
            writer.flush();
            writer.close();
        } catch (IOException ignored) {
        }
        writer = null;
    }

    private static void mirror(Level level, String uiLine) {
        if (level == Level.ERROR) {
            System.err.println(uiLine);
        } else {
            System.out.println(uiLine);
        }
    }

    private void appendUi(String line) {
        JTextArea area = console;
        if (area == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            area.append(line + '\n');
            area.setCaretPosition(area.getDocument().getLength());
        });
    }

    private AppLogger() {
    }
}
