import bm.b0b0b0.hacks.ruBstatsHack;
import bm.b0b0b0.util.gui.Conf;
import javax.swing.*;
import java.nio.file.Path;

public class RunHack {
    public static void main(String[] args) throws Exception {
        Conf conf = new Conf();
        new ruBstatsHack(Path.of(args[0]), Path.of(args[1]), true, new JTextArea(), conf);
    }
}