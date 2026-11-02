package bm.b0b0b0.util.files;

import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.gui.Conf;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class CleanOutput {

    private CleanOutput() {
    }

    public static File write(File outputDir, String originalName, List<JarEntryUtils.Entry> entries, Conf conf)
            throws IOException {
        File outFile = CleanPipeline.outputFile(outputDir, originalName);
        b0b0b0Dick.log(String.format(conf.getTranslation("creatingCleanFile"), outFile.getAbsolutePath()));
        JarEntryUtils.writeJar(outFile, entries);
        report(outFile, conf);
        return outFile;
    }

    public static void report(File outFile, Conf conf) {
        if (outFile.exists() && outFile.length() > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("cleanFileCreatedSuccessfully"), outFile.getAbsolutePath()));
        } else {
            b0b0b0Dick.log(String.format(conf.getTranslation("failedToCreateCleanFile"), outFile.getAbsolutePath()));
        }
    }
}
