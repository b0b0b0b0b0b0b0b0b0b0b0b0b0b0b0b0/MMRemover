package bm.b0b0b0.util.dev;

import bm.b0b0b0.util.asm.VerifyStubClassLoader;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class JvmVerify {

    private static final String[][] TARGETS = {
            {"out-test/FastAsyncWorldEdit-Bukkit-2.4.8.jar", "com/sk89q/worldedit/bukkit/WorldEditPlugin"},
            {"out-test/ProtocolLib.jar", "com/comphenix/protocol/ProtocolLib"},
            {"out-test/WorldGuard.jar", "com/sk89q/worldguard/bukkit/WorldGuardPlugin"},
            {"out-test/Multiverse-Core-4.3.1.jar", "com/onarandombox/MultiverseCore/MultiverseCore"},
            {"out-test/EssentialsX-2.21.1-dev+12-8455212.jar", "com/earth2me/essentials/Essentials"},
            {"out-test/MyCommand.jar", "it/emmerrei/mycommand/Main"},
            {"out-test/MLSAC-1.0-b42-java17.jar", "wtf/mlsac/Main"},
            {"out-test/LPX  v3.7.11.jar", "it/ytnoos/lpx/AD"},
            {"out-test/ExploitFixer v3.3.6.jar", "com/arkflame/exploitfixer/ExploitFixer"},
            {"out-test/AntiWorldFly-2.5.4.jar", "com/hm/antiworldfly/AntiWorldFly"}
    };

    public static void main(String[] args) throws Exception {
        var root = new File(System.getProperty("user.dir"));
        List<String> errors = new ArrayList<>();

        for (String[] t : TARGETS) {
            var jar = new File(root, t[0]);
            if (!jar.isFile()) {
                errors.add(t[0] + ": file missing");
                continue;
            }
            String binary = t[1].replace('/', '.');
            try {
                verifyJarClass(jar, binary);
                System.out.println("OK: " + t[0] + " :: " + binary);
            } catch (VerifyError ve) {
                errors.add(t[0] + " :: " + binary + " :: VerifyError: " + ve.getMessage());
            } catch (Throwable ex) {
                errors.add(t[0] + " :: " + binary + " :: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }

        if (errors.isEmpty()) {
            System.out.println("JVM verify: all targets passed");
        } else {
            System.err.println("JVM verify FAIL: " + errors.size());
            errors.forEach(System.err::println);
            System.exit(1);
        }
    }

    public static void defineAndVerify(String binaryName, byte[] bytes) {
        Map<String, byte[]> single = new HashMap<>();
        String internal = binaryName.replace('.', '/');
        single.put(internal, bytes);
        VerifyStubClassLoader loader = new VerifyStubClassLoader(single, JvmVerify.class.getClassLoader());
        loader.define(binaryName, bytes);
    }

    public static void verifyJarClass(File jarFile, String binaryName) throws Exception {
        Map<String, byte[]> classes = loadJarClasses(jarFile);
        VerifyStubClassLoader loader = new VerifyStubClassLoader(classes, JvmVerify.class.getClassLoader());
        loader.predefineJarClasses();
        loader.verifyClass(binaryName);
    }

    private static Map<String, byte[]> loadJarClasses(File jarFile) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        try (ZipFile zip = new ZipFile(jarFile)) {
            for (ZipEntry entry : zip.stream().toList()) {
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                byte[] bytes;
                try (InputStream in = zip.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }
                String internal = entry.getName().substring(0, entry.getName().length() - ".class".length());
                classes.put(internal, bytes);
                classes.put(entry.getName(), bytes);
            }
        }
        return classes;
    }
}
