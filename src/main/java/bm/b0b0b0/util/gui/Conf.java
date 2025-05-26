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

        Map<String, String> ruDefaults = Map.ofEntries(
                Map.entry("chooseFolderButton", "Выбрать папку"),
                Map.entry("removeAllButton", "Начать очистку"),
                Map.entry("refreshFilesButton", "Обновить список файлов"),
                Map.entry("openInputFolderButton", "Открыть папку 'input'"),
                Map.entry("openOutputFolderButton", "Открыть папку 'out'"),
                Map.entry("clearInputListButton", "Очистить input"),
                Map.entry("clearOutputListButton", "Очистить out"),
                Map.entry("clearConsoleButton", "Очистить консоль"),
                Map.entry("consolePanelTitle", "Консоль"),
                Map.entry("inputPanelTitle", "Сюда плагин с хаком"),
                Map.entry("outputPanelTitle", "Отсюда забираешь готовое"),
                Map.entry("pageTitle", "Страница программы"),
                Map.entry("workingDirSet", "Рабочая директория установлена в: "),
                Map.entry("workingDirFailed", "Не удалось установить рабочую директорию."),
                Map.entry("iconNotFound", "Иконка не найдена"),
                Map.entry("folderNotFound", "Папка не найдена или это не директория."),
                Map.entry("checkingFolderExists", "Проверка существования папки"),
                Map.entry("refreshFiles", "Обновление файлов"),
                Map.entry("rel_file_start", "Обновление списка файлов при запуске"),
                Map.entry("confirmClearInputList", "Вы уверены, что хотите очистить список 'input' и удалить все файлы?"),
                Map.entry("confirmClearOutputList", "Вы уверены, что хотите очистить список 'out' и удалить все файлы?"),
                Map.entry("confirmClearConsole", "Вы уверены, что хотите очистить консоль?"),
                Map.entry("confirmActionTitle", "Подтверждение действия"),
                Map.entry("bot_bm", "Проверить плагины на вирусы")
        );

        Map<String, String> enDefaults = Map.ofEntries(
                Map.entry("chooseFolderButton", "Choose Folder"),
                Map.entry("removeAllButton", "Start Cleanup"),
                Map.entry("refreshFilesButton", "Refresh Files"),
                Map.entry("openInputFolderButton", "Open Input Folder"),
                Map.entry("openOutputFolderButton", "Open Output Folder"),
                Map.entry("clearInputListButton", "Clear Input"),
                Map.entry("clearOutputListButton", "Clear Output"),
                Map.entry("clearConsoleButton", "Clear Console"),
                Map.entry("consolePanelTitle", "Console"),
                Map.entry("inputPanelTitle", "Input"),
                Map.entry("outputPanelTitle", "Output"),
                Map.entry("pageTitle", "Plugin Page"),
                Map.entry("workingDirSet", "Working directory set to "),
                Map.entry("workingDirFailed", "Failed to set working dir"),
                Map.entry("iconNotFound", "Icon not found"),
                Map.entry("folderNotFound", "Folder not found or is not a directory."),
                Map.entry("checkingFolderExists", "Checking folder"),
                Map.entry("refreshFiles", "Refreshing file list"),
                Map.entry("rel_file_start", "Program started"),
                Map.entry("confirmClearInputList", "Clear input folder?"),
                Map.entry("confirmClearOutputList", "Clear output folder?"),
                Map.entry("confirmClearConsole", "Clear console?"),
                Map.entry("confirmActionTitle", "Are you sure?"),
                Map.entry("bot_bm", "Check plugins for viruses")

        );

        Map<String, String> ruTranslations = (Map<String, String>) config.get("ru");
        Map<String, String> enTranslations = (Map<String, String>) config.get("en");

        ensureAllKeysPresent(Paths.get(CONFIG_FILE), ruTranslations, ruDefaults);
        ensureAllKeysPresent(Paths.get(CONFIG_FILE), enTranslations, enDefaults);

        config.put("ru", ruTranslations);
        config.put("en", enTranslations);

        saveConfig();
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
    private void ensureAllKeysPresent(Path configPath, Map<String, String> current, Map<String, String> defaults) {
        boolean updated = false;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                current.put(entry.getKey(), entry.getValue());
                updated = true;
            }
        }
        if (updated) {
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(configPath))) {
                for (Map.Entry<String, String> entry : current.entrySet()) {
                    writer.println(entry.getKey() + ": \"" + entry.getValue().replace("\"", "\\\"") + "\"");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
