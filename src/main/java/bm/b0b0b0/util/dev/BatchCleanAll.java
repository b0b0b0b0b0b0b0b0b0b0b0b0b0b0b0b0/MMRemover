package bm.b0b0b0.util.dev;

import bm.b0b0b0.hacks.Artemka;
import bm.b0b0b0.hacks.HostFlow;
import bm.b0b0b0.hacks.PluginMetrics;
import bm.b0b0b0.hacks.aph;
import bm.b0b0b0.hacks.bStatsJar;
import bm.b0b0b0.hacks.chbkHack;
import bm.b0b0b0.hacks.ruBstatsHack;
import bm.b0b0b0.hacks.systemMetricsHack;
import bm.b0b0b0.util.files.CleanPipeline;
import bm.b0b0b0.util.gui.Conf;

import java.io.File;
import java.nio.file.Path;

public final class BatchCleanAll {

    private static final String[] ALL_TYPES = {
            "hostflow", "artemka", "bstatsjar", "pluginmetrics", "aph",
            "ruBstatsHack", "systemMetricsHack", "chbkHack"
    };

    public static void main(String[] args) throws Exception {
        Path inputPath = new File(System.getProperty("user.dir"), "input").toPath();
        Path outputPath = new File(System.getProperty("user.dir"), "out").toPath();
        outputPath.toFile().mkdirs();

        Conf conf = new Conf();
        CleanPipeline.beginUnified(outputPath.toFile());
        try {
            for (String type : ALL_TYPES) {
                System.out.println("Phase: " + type);
                runHack(inputPath, outputPath, type, conf);
            }
            System.out.println("Done -> " + outputPath.toAbsolutePath());
        } finally {
            CleanPipeline.endUnified();
        }
    }

    private static void runHack(Path inputPath, Path outputPath, String type, Conf conf) {
        switch (type.toLowerCase()) {
            case "hostflow" -> new HostFlow(inputPath, outputPath, true, null, conf);
            case "artemka" -> new Artemka(inputPath, outputPath, true, null, conf);
            case "bstatsjar" -> new bStatsJar(inputPath, outputPath, true, null, conf);
            case "pluginmetrics" -> new PluginMetrics(inputPath, outputPath, true, null, conf);
            case "aph" -> new aph(inputPath, outputPath, true, null, conf);
            case "chbkhack" -> new chbkHack(inputPath, outputPath, true, null, conf);
            case "rubstatshack" -> new ruBstatsHack(inputPath, outputPath, true, null, conf);
            case "systemmetricshack" -> new systemMetricsHack(inputPath, outputPath, true, null, conf);
            default -> throw new IllegalArgumentException(type);
        }
    }
}
