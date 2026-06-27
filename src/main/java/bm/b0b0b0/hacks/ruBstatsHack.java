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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Enumeration;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static bm.b0b0b0.util.files.FileUtils.generateUniqueFileName;

public class ruBstatsHack {

    private static final String MALWARE_PREFIX = "ru/bstats/";
    private static final byte[] MALWARE_REPORT_URL = "api-bstats.online/api/v2/data/%s".getBytes(StandardCharsets.UTF_8);
    private static final byte[] MALWARE_DOMAIN = "api-bstats.online".getBytes(StandardCharsets.UTF_8);
    private static final byte[] MALWARE_PKG = "ru/bstats".getBytes(StandardCharsets.UTF_8);

    private final List<String> infectedFiles = new CopyOnWriteArrayList<>();
    private final Path outputPath;
    private final ExecutorService executor;
    private final Conf conf;

    public ruBstatsHack(Path pluginsPath, Path outputPath,
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

        try (ZipFile zip = new ZipFile(file)) {
            b0b0b0Dick.log(String.format(conf.getTranslation("scanningFiles"), file.getName()));

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if ("ru/bstats/InternalService.class".equals(name)) {
                    tagInfected(file, "InternalService.class");
                    return;
                }
                if ("ru/bstats/Metrics.class".equals(name)) {
                    tagInfected(file, "Metrics.class");
                    return;
                }
                if (name.startsWith(MALWARE_PREFIX) && name.endsWith(".class")) {
                    tagInfected(file, name);
                    return;
                }

                if (!name.endsWith(".class")) {
                    continue;
                }

                try (var in = zip.getInputStream(entry)) {
                    byte[] data = in.readAllBytes();

                    if (indexOf(data, MALWARE_REPORT_URL) != -1) {
                        tagInfected(file, String.format(conf.getTranslation("infectionBstatsApi"), name));
                        return;
                    }
                    if (indexOf(data, MALWARE_DOMAIN) != -1) {
                        tagInfected(file, String.format(conf.getTranslation("infectionBstatsDomain"), name));
                        return;
                    }
                    if (referencesRuBstats(data)) {
                        tagInfected(file, String.format(conf.getTranslation("infectionRuBstatsCall"), name));
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            b0b0b0Dick.log(String.format(conf.getTranslation("errorScanningFiles"), file.getName()));
        }
    }

    private void tagInfected(File file, String reason) {
        if (infectedFiles.contains(file.getAbsolutePath())) {
            return;
        }
        infectedFiles.add(file.getAbsolutePath());
        b0b0b0Dick.log(String.format(conf.getTranslation("ruBstatsInfected"), reason, file.getName()));
    }

    private boolean referencesRuBstats(byte[] clazz) {
        if (indexOf(clazz, MALWARE_PKG) == -1 && indexOf(clazz, MALWARE_DOMAIN) == -1) {
            return false;
        }

        try {
            ClassReader cr = new ClassReader(clazz);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            for (MethodNode method : cn.methods) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof MethodInsnNode min && min.owner.startsWith("ru/bstats")) {
                        return true;
                    }
                    if (insn instanceof TypeInsnNode tin
                            && (tin.getOpcode() == Opcodes.NEW || tin.getOpcode() == Opcodes.CHECKCAST)
                            && tin.desc.startsWith("ru/bstats")) {
                        return true;
                    }
                    if (insn instanceof FieldInsnNode fin && fin.owner.startsWith("ru/bstats")) {
                        return true;
                    }
                    if (insn instanceof LdcInsnNode ldc) {
                        if (ldc.cst instanceof String s && s.contains("api-bstats.online")) {
                            return true;
                        }
                        if (ldc.cst instanceof Type type && type.getInternalName().startsWith("ru/bstats")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private void runRemover(File file, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException(String.format(conf.getTranslation("outputDirCreateFailed"), outputDir));
        }

        List<JarEntryUtils.Entry> all = JarEntryUtils.readDeduped(file);
        int dupes = countDuplicateEntries(file) - all.size();
        if (dupes > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("zipDuplicatesMerged"), dupes));
        }

        List<JarEntryUtils.Entry> filtered = new ArrayList<>();
        for (JarEntryUtils.Entry entry : all) {
            if (shouldSkipEntry(entry.name())) {
                b0b0b0Dick.log(String.format(conf.getTranslation("removingFile"), entry.name()));
                continue;
            }
            if (isBrokenSignatureEntry(entry.name())) {
                continue;
            }

            if (!entry.isDirectory() && entry.name().endsWith(".class")) {
                byte[] cleaned = stripRuBstatsReferences(entry.data());
                filtered.add(new JarEntryUtils.Entry(entry.name(), false, cleaned));
            } else {
                filtered.add(entry);
            }
        }

        File outFile = new File(outputDir, generateUniqueFileName(outputDir, file.getName()));
        b0b0b0Dick.log(String.format(conf.getTranslation("creatingCleanFile"), outFile.getAbsolutePath()));
        JarEntryUtils.writeJar(outFile, removeEmptyDirectories(filtered));

        if (outFile.length() > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("cleanFileCreatedSuccessfully"), outFile.getAbsolutePath()));
        } else {
            b0b0b0Dick.log(String.format(conf.getTranslation("failedToCreateCleanFile"), outFile.getAbsolutePath()));
        }
    }

    private int countDuplicateEntries(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            int count = 0;
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                en.nextElement();
                count++;
            }
            return count;
        }
    }

    private boolean shouldSkipEntry(String name) {
        if (name.startsWith(MALWARE_PREFIX)) {
            return true;
        }
        if (name.startsWith("org/objectweb/asm/")) {
            return true;
        }
        return "asm.jar".equals(name) || "asm-tree.jar".equals(name);
    }

    private boolean isBrokenSignatureEntry(String name) {
        return name.startsWith("META-INF/")
                && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA"));
    }

    private byte[] stripRuBstatsReferences(byte[] clazz) {
        if (!needsProcessing(clazz)) {
            return clazz;
        }

        try {
            ClassReader cr = new ClassReader(clazz);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);

            boolean changed = false;
            for (MethodNode method : cn.methods) {
                if (stripMethodReferences(method)) {
                    changed = true;
                }
            }

            if (!changed) {
                return clazz;
            }

            ClassWriter cw = new ClassWriter(cr, 0);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable ignored) {
            return clazz;
        }
    }

    private boolean needsProcessing(byte[] data) {
        return indexOf(data, MALWARE_PKG) != -1 || indexOf(data, MALWARE_DOMAIN) != -1;
    }

    private boolean stripMethodReferences(MethodNode method) {
        List<AbstractInsnNode> trash = new ArrayList<>();

        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode min && min.owner.startsWith("ru/bstats")) {
                trash.add(insn);
                int argCount = Type.getArgumentTypes(min.desc).length;
                if (min.getOpcode() != Opcodes.INVOKESTATIC) {
                    argCount++;
                }
                collectArgumentLoads(trash, insn.getPrevious(), argCount);
                continue;
            }

            if (insn instanceof FieldInsnNode fin && fin.owner.startsWith("ru/bstats")) {
                trash.add(insn);
                if (fin.getOpcode() != Opcodes.GETSTATIC && fin.getOpcode() != Opcodes.PUTSTATIC) {
                    AbstractInsnNode prev = insn.getPrevious();
                    if (prev != null) {
                        trash.add(prev);
                    }
                }
                continue;
            }

            if (insn instanceof TypeInsnNode tin
                    && tin.getOpcode() == Opcodes.NEW
                    && tin.desc.startsWith("ru/bstats")) {
                trash.add(insn);
                for (AbstractInsnNode cur = insn.getNext(); cur != null; cur = cur.getNext()) {
                    trash.add(cur);
                    int op = cur.getOpcode();
                    if (op == Opcodes.POP || op == Opcodes.POP2 || op == Opcodes.ASTORE || op == Opcodes.INVOKESPECIAL) {
                        break;
                    }
                }
            }
        }

        if (trash.isEmpty()) {
            return false;
        }

        trash.forEach(method.instructions::remove);
        return true;
    }

    private void collectArgumentLoads(List<AbstractInsnNode> trash, AbstractInsnNode start, int argCount) {
        AbstractInsnNode cur = start;
        int collected = 0;
        while (cur != null && collected < argCount) {
            trash.add(cur);
            collected++;
            cur = cur.getPrevious();
        }
    }

    private List<JarEntryUtils.Entry> removeEmptyDirectories(List<JarEntryUtils.Entry> list) {
        Set<String> nonEmpty = new HashSet<>();
        for (JarEntryUtils.Entry entry : list) {
            if (!entry.isDirectory()) {
                String path = entry.name();
                for (int idx = path.lastIndexOf('/'); idx != -1; idx = path.lastIndexOf('/', idx - 1)) {
                    nonEmpty.add(path.substring(0, idx + 1));
                }
            }
        }
        List<JarEntryUtils.Entry> res = new ArrayList<>();
        for (JarEntryUtils.Entry entry : list) {
            if (!entry.isDirectory() || nonEmpty.contains(entry.name())) {
                res.add(entry);
            }
        }
        return res;
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
}
