package bm.b0b0b0.util.files;

import java.io.File;

public final class CleanPipeline {

    private static final ThreadLocal<Boolean> UNIFIED = ThreadLocal.withInitial(() -> false);

    private CleanPipeline() {
    }

    public static void beginUnified(File outputDir) {
        UNIFIED.set(true);
        if (!outputDir.exists()) {
            return;
        }
        File[] files = outputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            file.delete();
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
