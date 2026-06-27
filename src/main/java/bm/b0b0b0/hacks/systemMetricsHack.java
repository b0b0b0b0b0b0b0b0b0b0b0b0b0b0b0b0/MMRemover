package bm.b0b0b0.hacks;

import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.files.JarEntryUtils;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static bm.b0b0b0.util.files.FileUtils.generateUniqueFileName;

public class systemMetricsHack {

    private static final String[] YAML_DESCRIPTORS = {
            "plugin.yml",
            "paper-plugin.yml",
            "bungee.yml"
    };
    private static final String VELOCITY_DESCRIPTOR = "velocity-plugin.json";
    private static final Pattern JSON_MAIN = Pattern.compile("\"main\"\\s*:\\s*\"([^\"\\\\]+)\"");
    private static final String METRICS_METHOD_NAME = "SystemMetrics";
    private static final String METRICS_METHOD_DESC = "(Lorg/bukkit/plugin/java/JavaPlugin;)V";
    private static final byte[] PANEL_BSTATS_URL = "panel.bstats.co".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ON_ENABLE_INJ = "onEnableInj".getBytes(StandardCharsets.UTF_8);
    private static final byte[] RELOCATE_CHECKS = "bstats.relocatechecks".getBytes(StandardCharsets.UTF_8);

    private final List<String> infectedFiles = new CopyOnWriteArrayList<>();
    private final Path outputPath;
    private final ExecutorService executor;
    private final Conf conf;

    public systemMetricsHack(Path pluginsPath, Path outputPath,
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
                    .forEach(p -> executor.submit(() -> scanFile(p.toFile())));

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            b0b0b0Dick.reportMalware(infectedFiles, consoleArea, conf);

            if (removeMalware) {
                cleanInfectedFiles();
            }
            b0b0b0Dick.log(conf.getTranslation("done"));
        } catch (Exception e) {
            e.printStackTrace();
            b0b0b0Dick.log(conf.getTranslation("error") + ": " + e.getMessage());
        }
    }

    private void cleanInfectedFiles() {
        for (String path : infectedFiles) {
            try {
                File src = new File(path);
                b0b0b0Dick.log(conf.getTranslation("cleaningFile") + ": " + src.getAbsolutePath());
                runRemover(src, outputPath.toFile());
            } catch (Throwable t) {
                b0b0b0Dick.log(conf.getTranslation("errorCleaningFile") + ": " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    private void scanFile(File file) {
        if (!file.getName().endsWith(".jar")) {
            return;
        }

        try {
            b0b0b0Dick.log(String.format(conf.getTranslation("scanningFiles"), file.getName()));

            for (String mainClass : readMainClasses(file)) {
                String mainEntry = toClassEntry(mainClass);
                byte[] mainBytecode = readClassEntry(file, mainEntry);
                if (mainBytecode != null && isInfectedMainClass(mainBytecode)) {
                    tagInfected(file, mainEntry);
                    return;
                }
            }
        } catch (Throwable t) {
            b0b0b0Dick.log(String.format(conf.getTranslation("errorScanningFiles"), file.getName()));
        }
    }

    private List<String> readMainClasses(File jar) throws IOException {
        Set<String> mains = new LinkedHashSet<>();
        try (ZipFile zip = new ZipFile(jar)) {
            for (String descriptor : YAML_DESCRIPTORS) {
                ZipEntry entry = zip.getEntry(descriptor);
                if (entry == null) {
                    continue;
                }
                String yml = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                String main = parseYamlMain(yml);
                if (main != null) {
                    mains.add(main);
                }
            }

            ZipEntry velocity = zip.getEntry(VELOCITY_DESCRIPTOR);
            if (velocity != null) {
                String json = new String(zip.getInputStream(velocity).readAllBytes(), StandardCharsets.UTF_8);
                String main = parseJsonMain(json);
                if (main != null) {
                    mains.add(main);
                }
            }
        }
        return new ArrayList<>(mains);
    }

    private String parseYamlMain(String yml) {
        for (String line : yml.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("main:")) {
                continue;
            }
            String value = trimmed.substring("main:".length()).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private String parseJsonMain(String json) {
        Matcher matcher = JSON_MAIN.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String toClassEntry(String mainClass) {
        return mainClass.replace('.', '/') + ".class";
    }

    private byte[] readClassEntry(File jar, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null || entry.isDirectory()) {
                return null;
            }
            return zip.getInputStream(entry).readAllBytes();
        }
    }

    private boolean isInfectedMainClass(byte[] data) {
        if (data.length == 0) {
            return false;
        }

        if (indexOf(data, PANEL_BSTATS_URL) != -1
                || indexOf(data, ON_ENABLE_INJ) != -1
                || indexOf(data, RELOCATE_CHECKS) != -1) {
            return true;
        }

        try {
            ClassReader cr = new ClassReader(data);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return cn.methods.stream().anyMatch(this::isMalwareMetricsMethod);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isMalwareMetricsMethod(MethodNode method) {
        return METRICS_METHOD_NAME.equals(method.name) && METRICS_METHOD_DESC.equals(method.desc);
    }

    private void tagInfected(File file, String classEntry) {
        if (infectedFiles.contains(file.getAbsolutePath())) {
            return;
        }
        infectedFiles.add(file.getAbsolutePath());
        b0b0b0Dick.log(String.format(conf.getTranslation("infectionSystemMetrics"), classEntry, file.getName()));
    }

    private void runRemover(File file, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException(String.format(conf.getTranslation("outputDirCreateFailed"), outputDir));
        }

        Set<String> entriesToClean = new HashSet<>();
        for (String mainClass : readMainClasses(file)) {
            String mainEntry = toClassEntry(mainClass);
            byte[] bytecode = readClassEntry(file, mainEntry);
            if (bytecode != null && isInfectedMainClass(bytecode)) {
                entriesToClean.add(mainEntry);
            }
        }
        if (entriesToClean.isEmpty()) {
            return;
        }

        List<JarEntryUtils.Entry> all = JarEntryUtils.readDeduped(file);
        int hollowClassCount = 0;
        for (JarEntryUtils.Entry entry : all) {
            if (!entry.isDirectory() && entry.name().endsWith(".class")
                    && (entry.data() == null || entry.data().length == 0)) {
                hollowClassCount++;
            }
        }
        if (hollowClassCount > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("hollowClassWarning"), hollowClassCount));
        }

        List<JarEntryUtils.Entry> cleaned = new ArrayList<>();
        for (JarEntryUtils.Entry entry : all) {
            if (isBrokenSignatureEntry(entry.name())) {
                continue;
            }
            if (!entry.isDirectory() && entriesToClean.contains(entry.name()) && entry.data() != null) {
                byte[] stripped = stripSystemMetrics(entry.data());
                cleaned.add(new JarEntryUtils.Entry(entry.name(), false, stripped));
            } else {
                cleaned.add(entry);
            }
        }

        File outFile = new File(outputDir, generateUniqueFileName(outputDir, file.getName()));
        b0b0b0Dick.log(String.format(conf.getTranslation("creatingCleanFile"), outFile.getAbsolutePath()));
        JarEntryUtils.writeJar(outFile, cleaned);

        if (outFile.length() > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("cleanFileCreatedSuccessfully"), outFile.getAbsolutePath()));
        } else {
            b0b0b0Dick.log(String.format(conf.getTranslation("failedToCreateCleanFile"), outFile.getAbsolutePath()));
        }
    }

    private byte[] stripSystemMetrics(byte[] clazz) {
        try {
            ClassReader cr = new ClassReader(clazz);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            boolean changed = false;
            Iterator<MethodNode> iterator = cn.methods.iterator();
            while (iterator.hasNext()) {
                MethodNode method = iterator.next();
                if (isMalwareMetricsMethod(method) || method.name.startsWith("lambda$SystemMetrics")) {
                    b0b0b0Dick.log(String.format(conf.getTranslation("removingMethod"), cn.name + "." + method.name));
                    iterator.remove();
                    changed = true;
                }
            }

            for (MethodNode method : cn.methods) {
                if (stripMetricsInvocations(method)) {
                    changed = true;
                }
            }

            if (!changed) {
                return clazz;
            }

            ClassWriter cw = new CustomClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable ignored) {
            return clazz;
        }
    }

    private boolean stripMetricsInvocations(MethodNode method) {
        List<AbstractInsnNode> trash = new ArrayList<>();

        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode min)) {
                continue;
            }
            if (!METRICS_METHOD_NAME.equals(min.name) || !METRICS_METHOD_DESC.equals(min.desc)) {
                continue;
            }

            trash.add(insn);
            int argCount = Type.getArgumentTypes(min.desc).length;
            if (min.getOpcode() != Opcodes.INVOKESTATIC) {
                argCount++;
            }
            collectStackLoads(trash, insn.getPrevious(), argCount);
        }

        if (trash.isEmpty()) {
            return false;
        }

        trash.forEach(method.instructions::remove);
        return true;
    }

    private void collectStackLoads(List<AbstractInsnNode> trash, AbstractInsnNode start, int count) {
        AbstractInsnNode cur = start;
        int collected = 0;
        while (cur != null && collected < count) {
            trash.add(cur);
            collected++;
            cur = cur.getPrevious();
        }
    }

    private boolean isBrokenSignatureEntry(String name) {
        return name.startsWith("META-INF/")
                && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA"));
    }

    private static int indexOf(byte[] hay, byte[] needle) {
        if (needle.length == 0 || hay.length < needle.length) {
            return -1;
        }
        outer:
        for (int i = 0; i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (hay[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static class CustomClassWriter extends ClassWriter {
        CustomClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return "java/lang/Object";
        }
    }
}
