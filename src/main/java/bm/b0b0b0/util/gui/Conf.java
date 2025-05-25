package bm.b0b0b0.util.gui;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Conf {
    private static final String CONFIG_FILE = "config.yml";
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

    private void loadConfig() {
        try (InputStream input = Files.newInputStream(Paths.get(CONFIG_FILE))) {
            Yaml yaml = new Yaml();
            config = yaml.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки конфигурации: " + e.getMessage(), e);
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
