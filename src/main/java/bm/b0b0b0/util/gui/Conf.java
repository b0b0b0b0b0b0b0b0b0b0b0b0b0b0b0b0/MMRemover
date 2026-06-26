package bm.b0b0b0.util.gui;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Conf {
    private static final String CONFIG_FILE = "config.yml";
    private static final int APP_CONFIG_VERSION = 3;
    private static final List<String> LANGUAGES = List.of("ru", "en");

    private Map<String, Object> config;

    public Conf() {
        createDefaultConfigIfNotExists();
        loadConfig();
    }

    private void createDefaultConfigIfNotExists() {
        Path configPath = Paths.get(CONFIG_FILE);
        if (!Files.exists(configPath)) {
            extractConfig();
        }
    }

    private void extractConfig() {
        Path configPath = Paths.get(CONFIG_FILE);
        try (InputStream resource = getClass().getResourceAsStream("/" + CONFIG_FILE)) {
            if (resource != null) {
                Files.copy(resource, configPath);
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка извлечения конфигурации: " + e.getMessage(), e);
        }

        try (InputStream zipStream = getClass().getProtectionDomain().getCodeSource().getLocation().openStream();
             ZipInputStream zis = new ZipInputStream(zipStream)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(CONFIG_FILE)) {
                    Files.copy(zis, configPath);
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка извлечения конфигурации: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> loadBundledConfig() {
        try (InputStream input = getClass().getResourceAsStream("/" + CONFIG_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Встроенный config.yml не найден");
            }
            Yaml yaml = new Yaml();
            Map<String, Object> bundled = yaml.load(input);
            if (bundled == null) {
                throw new IllegalStateException("Встроенный config.yml пуст");
            }
            return bundled;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки встроенной конфигурации: " + e.getMessage(), e);
        }
    }

    private void loadConfig() {
        try (InputStream input = Files.newInputStream(Paths.get(CONFIG_FILE))) {
            Yaml yaml = new Yaml();
            config = yaml.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки конфигурации: " + e.getMessage(), e);
        }

        if (config == null) {
            config = new LinkedHashMap<>();
        }

        Map<String, Object> bundled = loadBundledConfig();
        int userVersion = readConfigVersion(config.get("configVersion"));
        boolean overwriteTranslations = userVersion < APP_CONFIG_VERSION;

        mergeTranslations(bundled, overwriteTranslations);

        if (!config.containsKey("lang")) {
            config.put("lang", bundled.getOrDefault("lang", "ru"));
        }

        config.put("configVersion", APP_CONFIG_VERSION);
        saveConfig();
    }

    private int readConfigVersion(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private void mergeTranslations(Map<String, Object> bundled, boolean overwrite) {
        for (String language : LANGUAGES) {
            Map<String, String> userTranslations = (Map<String, String>) config.computeIfAbsent(
                    language, key -> new LinkedHashMap<>());
            Map<String, String> bundledTranslations = (Map<String, String>) bundled.get(language);
            if (bundledTranslations == null) {
                continue;
            }

            for (Map.Entry<String, String> entry : bundledTranslations.entrySet()) {
                if (overwrite || !userTranslations.containsKey(entry.getKey())) {
                    userTranslations.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public String getLang() {
        return (String) config.getOrDefault("lang", "ru");
    }

    public String getTranslation(String key) {
        String lang = getLang();
        Map<String, String> translations = (Map<String, String>) config.get(lang);
        if (translations != null) {
            return translations.getOrDefault(key, key);
        } else {
            return key;
        }
    }

    public void setLang(String lang) {
        config.put("lang", lang);
        saveConfig();
    }

    public void saveConfig() {
        try (Writer writer = Files.newBufferedWriter(Paths.get(CONFIG_FILE), StandardCharsets.UTF_8)) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            yaml.dump(config, writer);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения конфигурации: " + e.getMessage(), e);
        }
    }
}
