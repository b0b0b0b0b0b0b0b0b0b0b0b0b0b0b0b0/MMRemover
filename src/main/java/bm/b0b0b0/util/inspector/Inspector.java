package bm.b0b0b0.util.inspector;

import bm.b0b0b0.util.inspector.model.BatchReport;
import bm.b0b0b0.util.inspector.model.InspectionReport;
import bm.b0b0b0.util.inspector.scan.JarScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class Inspector {

    private Inspector() {
    }

    public static InspectionReport inspect(File jarFile) {
        return JarScanner.scan(jarFile);
    }

    public static BatchReport inspectAll(List<File> files) {
        return inspectAll(files, null);
    }

    public static BatchReport inspectAll(List<File> files, Consumer<String> progress) {
        BatchReport batch = new BatchReport();
        if (files == null) {
            return batch;
        }

        List<File> jars = new ArrayList<>();
        for (File file : files) {
            if (file == null) {
                continue;
            }
            String lower = file.getName().toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jar")) {
                jars.add(file);
            }
        }

        for (File jar : jars) {
            if (progress != null) {
                progress.accept(jar.getName());
            }
            batch.reports.add(JarScanner.scan(jar));
        }

        batch.aggregate();
        return batch;
    }
}
