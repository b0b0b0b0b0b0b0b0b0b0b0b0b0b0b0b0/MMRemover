package bm.b0b0b0.util.dev;

import bm.b0b0b0.hacks.chbkHack;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class BatchVerify {

    private static final String[] FAILING = {
            "FastAsyncWorldEdit-Bukkit-2.4.8.jar",
            "ProtocolLib.jar",
            "WorldGuard.jar",
            "Multiverse-Core-4.3.1.jar",
            "EssentialsX-2.21.1-dev+12-8455212.jar",
            "MyCommand.jar",
            "MLSAC-1.0-b42-java17.jar",
            "LPX  v3.7.11.jar",
            "ExploitFixer v3.3.6.jar",
            "AntiWorldFly-2.5.4.jar",
            "SIMSpecAlert-1.1-SQD.jar",
            "Vulcan-2.9.7-cracked.jar"
    };

    private static final String[][] TARGET_CLASSES = {
            {"FastAsyncWorldEdit-Bukkit-2.4.8.jar", "com/sk89q/worldedit/bukkit/WorldEditPlugin"},
            {"ProtocolLib.jar", "com/comphenix/protocol/ProtocolLib"},
            {"WorldGuard.jar", "com/sk89q/worldguard/bukkit/WorldGuardPlugin"},
            {"Multiverse-Core-4.3.1.jar", "com/onarandombox/MultiverseCore/MultiverseCore"},
            {"EssentialsX-2.21.1-dev+12-8455212.jar", "com/earth2me/essentials/Essentials"},
            {"MyCommand.jar", "it/emmerrei/mycommand/Main"},
            {"MLSAC-1.0-b42-java17.jar", "wtf/mlsac/Main"},
            {"LPX  v3.7.11.jar", "it/ytnoos/lpx/AD"},
            {"ExploitFixer v3.3.6.jar", "com/arkflame/exploitfixer/ExploitFixer"},
            {"AntiWorldFly-2.5.4.jar", "com/hm/antiworldfly/AntiWorldFly"},
            {"SIMSpecAlert-1.1-SQD.jar", "surc/simbay/simspecalert/main/SIMSpecAlert"},
            {"Vulcan-2.9.7-cracked.jar", "me/frep/vulcan/spigot/VulcanPlugin"}
    };

    public static void main(String[] args) throws Exception {
        File root = new File(System.getProperty("user.dir"));
        File input = new File(root, "input");
        File out = new File(root, "out-test");
        out.mkdirs();

        Conf conf = new Conf();
        List<String> errors = new ArrayList<>();
        bm.b0b0b0.util.files.CleanPipeline.beginUnified(out);

        try {
            for (String name : FAILING) {
                File src = new File(input, name);
                if (!src.isFile()) {
                    errors.add(name + ": missing in input/");
                    continue;
                }
                chbkHack.cleanJar(src, out, conf);
                File cleaned = new File(out, name);
                if (!cleaned.isFile()) {
                    errors.add(name + ": not produced in out-test/");
                    continue;
                }
                verifyJar(name, cleaned, errors);
            }
        } finally {
            bm.b0b0b0.util.files.CleanPipeline.endUnified();
        }

        for (String[] target : TARGET_CLASSES) {
            String jarName = target[0];
            String internal = target[1];
            File jar = new File(out, jarName);
            if (!jar.isFile()) {
                continue;
            }
            String entry = internal + ".class";
            try (ZipFile zip = new ZipFile(jar)) {
                ZipEntry e = zip.getEntry(entry);
                if (e == null) {
                    errors.add(jarName + ": missing target " + entry);
                    continue;
                }
                byte[] bytes = zip.getInputStream(e).readAllBytes();
                verifyClass(jarName, entry, bytes, errors);
            }
        }

        if (errors.isEmpty()) {
            System.out.println("OK: all " + FAILING.length + " JARs verified");
        } else {
            System.err.println("FAIL: " + errors.size() + " issue(s)");
            errors.forEach(System.err::println);
            System.exit(1);
        }
    }

    private static void verifyJar(String jarName, File jar, List<String> errors) throws Exception {
        try (ZipFile zip = new ZipFile(jar)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory() || !e.getName().endsWith(".class")) {
                    continue;
                }
                byte[] bytes = zip.getInputStream(e).readAllBytes();
                verifyClass(jarName, e.getName(), bytes, errors);
            }
        }
    }

    private static void verifyClass(String jarName, String entry, byte[] bytes, List<String> errors) {
        try {
            ClassReader cr = new ClassReader(bytes);
            CheckClassAdapter.verify(cr, false, new PrintWriterAdapter(jarName, entry));
        } catch (Throwable ex) {
            errors.add(jarName + " :: " + entry + " :: " + ex.getMessage());
        }
    }

    private static final class PrintWriterAdapter extends java.io.PrintWriter {
        private final String jar;
        private final String cls;

        PrintWriterAdapter(String jar, String cls) {
            super(new java.io.StringWriter());
            this.jar = jar;
            this.cls = cls;
        }

        @Override
        public void println(String x) {
            throw new RuntimeException(jar + " :: " + cls + " :: " + x);
        }
    }
}
