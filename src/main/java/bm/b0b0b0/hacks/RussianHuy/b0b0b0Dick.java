package bm.b0b0b0.hacks.RussianHuy;

import bm.b0b0b0.util.gui.Conf;
import bm.b0b0b0.util.files.FileUtils;

import javax.swing.JTextArea;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipOutputStream;

public final class b0b0b0Dick {
    private static volatile JTextArea consoleArea;
    public static void setConsole(JTextArea area) {
        consoleArea = area;
    }
    public static void log(String message) {
        String line = FileUtils.formatLogLine(message);
        System.out.println(line);
        JTextArea area = consoleArea;
        if (area != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                area.append(line + '\n');
                area.setCaretPosition(area.getDocument().getLength());
            });
        }
    }
    public static void reportMalware(List<String> infectedFiles, JTextArea areaFromUI, Conf conf) {

        if (consoleArea == null && areaFromUI != null) {
            setConsole(areaFromUI);
        }

        String msg;
        int cnt = infectedFiles.size();

        if (cnt == 0) {
            msg = conf.getTranslation("noMalwareFound");
        } else {
            String word;
            if (cnt % 100 >= 11 && cnt % 100 <= 19) {
                word = conf.getTranslation("pluginsPlural");
            } else {
                word = switch (cnt % 10) {
                    case 1  -> conf.getTranslation("pluginSingular");
                    case 2,3,4 -> conf.getTranslation("pluginsFew");
                    default -> conf.getTranslation("pluginsPlural");
                };
            }
            msg = String.format(conf.getTranslation("foundMalware"), cnt, word);
        }
        log(msg);
    }
    public static void writeZipEntry(ZipOutputStream out, InputStream in) throws Throwable {
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        out.closeEntry();
    }
    private b0b0b0Dick() {}
}
