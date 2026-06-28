package bm.b0b0b0.util.update;

import bm.b0b0b0.util.AppLinks;
import bm.b0b0b0.util.files.FileUtils;
import bm.b0b0b0.util.gui.Conf;
import bm.b0b0b0.util.gui.UpdateDialog;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {
    private static final String VERSION_URL = "https://b0b0b0.dev/mm/v.json";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;

    private UpdateChecker() {
    }

    public static void checkForUpdates(Conf conf, Component parent, JTextArea console) {
        log(console, conf.getTranslation("updateCheckStart"));
        Thread thread = new Thread(() -> {
            try {
                UpdateInfo update = fetchLatest();
                if (update == null) {
                    log(console, conf.getTranslation("updateCheckSkipped"));
                    return;
                }
                if (!VersionCompare.isNewer(update.version(), AppVersion.CURRENT)) {
                    log(console, conf.getTranslation("updateCheckLatest"));
                    return;
                }
                log(console, String.format(conf.getTranslation("updateCheckFound"), update.version()));
                SwingUtilities.invokeLater(() -> UpdateDialog.show(parent, conf, update));
            } catch (Exception ignored) {
                log(console, conf.getTranslation("updateCheckSkipped"));
            }
        }, "mmremover-update-check");
        thread.setDaemon(true);
        thread.start();
    }

    private static void log(JTextArea console, String message) {
        if (console == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> FileUtils.appendToConsole(console, message));
    }

    private static UpdateInfo fetchLatest() throws Exception {
        HttpURLConnection connection = open(VERSION_URL);
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                return null;
            }
            String contentType = connection.getContentType();
            if (contentType != null && !contentType.contains("json") && contentType.contains("html")) {
                return null;
            }
            String body = readBody(connection);
            if (body == null || body.isBlank() || body.trim().startsWith("<")) {
                return null;
            }
            return parse(body);
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "MMRemover/" + AppVersion.CURRENT);
        return connection;
    }

    private static String readBody(HttpURLConnection connection) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString().trim();
        }
    }

    private static UpdateInfo parse(String json) {
        String version = firstString(json, "version", "latest", "latestVersion");
        if (version == null || version.isBlank()) {
            return null;
        }
        String url = firstString(json, "url", "download", "downloadUrl", "download_url", "link", "page");
        if (url == null || url.isBlank()) {
            url = AppLinks.PROGRAM_PAGE;
        }
        String message = firstString(json, "message", "notes", "changelog", "description", "text");
        return new UpdateInfo(version.trim(), url.trim(), message != null ? message.trim() : "");
    }

    private static String firstString(String json, String... keys) {
        for (String key : keys) {
            String value = jsonString(json, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String jsonString(String json, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJson(matcher.group(1));
    }

    private static String unescapeJson(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    public static boolean openInBrowser(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
