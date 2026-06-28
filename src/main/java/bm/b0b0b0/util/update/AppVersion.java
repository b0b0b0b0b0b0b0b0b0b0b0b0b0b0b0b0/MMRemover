package bm.b0b0b0.util.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppVersion {
    public static final String CURRENT = load();

    private AppVersion() {
    }

    private static String load() {
        Package pkg = AppVersion.class.getPackage();
        if (pkg != null) {
            String fromManifest = pkg.getImplementationVersion();
            if (isValid(fromManifest)) {
                return fromManifest.trim();
            }
        }

        try (InputStream in = AppVersion.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (in != null) {
                Properties properties = new Properties();
                properties.load(in);
                String fromProperties = properties.getProperty("version");
                if (isValid(fromProperties)) {
                    return fromProperties.trim();
                }
            }
        } catch (IOException ignored) {
        }

        return "dev";
    }

    private static boolean isValid(String version) {
        return version != null
                && !version.isBlank()
                && !version.contains("${");
    }
}
