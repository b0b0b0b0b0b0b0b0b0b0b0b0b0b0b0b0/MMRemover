package bm.b0b0b0.util.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Chains multiple cleaners onto one output JAR during "clean all" runs.
 */
public final class CleanPipeline {

    private static final ThreadLocal<Boolean> UNIFIED = ThreadLocal.withInitial(() -> false);

    private CleanPipeline() {
    }

    public static void beginUnified(File inputDir, File outputDir) {
        UNIFIED.set(true);
        if (outputDir == null) {
            return;
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            return;
        }
        seedOutputFromInput(inputDir, outputDir);
    }

    public static void beginUnified(File outputDir) {
        beginUnified(null, outputDir);
    }

    private static void seedOutputFromInput(File inputDir, File outputDir) {
        if (inputDir == null || !inputDir.isDirectory()) {
            return;
        }
        File[] inputJars = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (inputJars == null) {
            return;
        }
        for (File jar : inputJars) {
            File dest = new File(outputDir, jar.getName());
            try {
                Files.copy(jar.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
        }
    }

    public static void endUnified() {
        UNIFIED.remove();
    }

    public static boolean isUnified() {
        return UNIFIED.get();
    }

    public static File sourceFile(File inputJar, File outputDir) {
        if (!isUnified()) {
            return inputJar;
        }
        File partial = new File(outputDir, inputJar.getName());
        return partial.exists() ? partial : inputJar;
    }

    public static File outputFile(File outputDir, String originalName) {
        if (isUnified()) {
            return new File(outputDir, originalName);
        }
        return new File(outputDir, FileUtils.generateUniqueFileName(outputDir, originalName));
    }
}
