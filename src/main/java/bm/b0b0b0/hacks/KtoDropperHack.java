package bm.b0b0b0.hacks;

import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.files.CleanPipeline;
import bm.b0b0b0.util.files.JarEntryUtils;
import bm.b0b0b0.util.gui.Conf;

import javax.swing.JTextArea;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KtoDropperHack {

    private final List<String> infectedFiles = new CopyOnWriteArrayList<>();
    private final Path outputPath;
    private final ExecutorService executor;
    private final Conf conf;

    public KtoDropperHack(Path pluginsPath, Path outputPath,
                          boolean removeMalware,
                          JTextArea consoleArea,
                          Conf conf) {
        this.conf = conf;
        this.outputPath = outputPath;
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            b0b0b0Dick.log(conf.getTranslation("scanningDirectory") + ": " + pluginsPath);

            Files.walk(pluginsPath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> executor.submit(() -> scanFile(path.toFile())));

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            b0b0b0Dick.reportMalware(infectedFiles, consoleArea, conf);

            if (removeMalware) {
                cleanInfectedFiles();
            }
            b0b0b0Dick.log(conf.getTranslation("done"));
        } catch (Exception exception) {
            exception.printStackTrace();
            b0b0b0Dick.log(conf.getTranslation("error") + ": " + exception.getMessage());
        }
    }

    private void cleanInfectedFiles() {
        for (String path : infectedFiles) {
            try {
                File src = new File(path);
                b0b0b0Dick.log(conf.getTranslation("cleaningFile") + ": " + src.getAbsolutePath());
                runRemover(src, outputPath.toFile());
            } catch (Throwable throwable) {
                b0b0b0Dick.log(conf.getTranslation("errorCleaningFile") + ": " + throwable.getMessage());
                throwable.printStackTrace();
            }
        }
    }

    private void scanFile(File file) {
        if (CleanPipeline.isRejected(file.getName())) {
            return;
        }
        if (!file.getName().endsWith(".jar")) {
            return;
        }
        try {
            b0b0b0Dick.log(String.format(conf.getTranslation("scanningFiles"), file.getName()));
            List<JarEntryUtils.Entry> entries = JarEntryUtils.readDeduped(file);
            if (KtoDropperCleaner.infected(entries)) {
                tagInfected(file);
            }
        } catch (Throwable throwable) {
            b0b0b0Dick.log(String.format(conf.getTranslation("errorScanningFiles"), file.getName()));
        }
    }

    private void tagInfected(File file) {
        if (infectedFiles.contains(file.getAbsolutePath())) {
            return;
        }
        infectedFiles.add(file.getAbsolutePath());
        b0b0b0Dick.log(String.format(
                conf.getTranslation("infectionKtoDropper"),
                KtoDropperMalwareDetector.REPORT,
                file.getName()));
    }

    private void runRemover(File file, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException(String.format(conf.getTranslation("outputDirCreateFailed"), outputDir));
        }

        File src = CleanPipeline.sourceFile(file, outputDir);
        List<JarEntryUtils.Entry> entries = JarEntryUtils.readDeduped(src);
        KtoDropperCleaner.Result result = KtoDropperCleaner.clean(entries);
        if (!result.infected()) {
            return;
        }
        if (result.error() != null) {
            b0b0b0Dick.log(String.format(conf.getTranslation("ktoDropperUnsafe"), file.getName(), result.error()));
            return;
        }

        for (String name : result.removed()) {
            b0b0b0Dick.log(String.format(conf.getTranslation("removingFile"), name));
        }
        for (String name : result.patched()) {
            b0b0b0Dick.log(String.format(conf.getTranslation("ktoDropperNeutralized"), name, result.sites()));
        }

        File outFile = CleanPipeline.outputFile(outputDir, file.getName());
        b0b0b0Dick.log(String.format(conf.getTranslation("creatingCleanFile"), outFile.getAbsolutePath()));
        JarEntryUtils.writeJar(outFile, new ArrayList<>(result.entries()));
        if (outFile.length() > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("cleanFileCreatedSuccessfully"), outFile.getAbsolutePath()));
        } else {
            b0b0b0Dick.log(String.format(conf.getTranslation("failedToCreateCleanFile"), outFile.getAbsolutePath()));
        }
    }
}
