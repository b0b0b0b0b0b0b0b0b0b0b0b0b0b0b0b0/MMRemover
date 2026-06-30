package bm.b0b0b0.hacks;

import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.files.JarEntryUtils;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static bm.b0b0b0.util.files.FileUtils.generateUniqueFileName;

public class ruBstatsHack {

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

            List<String> entryNames = new ArrayList<>();
            List<byte[]> classFiles = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                entryNames.add(entry.getName());
                if (entry.getName().endsWith(".class")) {
                    try (var in = zip.getInputStream(entry)) {
                        classFiles.add(in.readAllBytes());
                    } catch (Throwable ignored) {
                    }
                }
            }

            FakeBstatsMalwareDetector.Analysis analysis = FakeBstatsMalwareDetector.analyzeJar(entryNames, classFiles);
            if (analysis.infected()) {
                tagInfected(file, analysis.reason());
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

    private void runRemover(File file, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException(String.format(conf.getTranslation("outputDirCreateFailed"), outputDir));
        }

        List<JarEntryUtils.Entry> all = JarEntryUtils.readDeduped(file);
        int dupes = countDuplicateEntries(file) - all.size();
        if (dupes > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("zipDuplicatesMerged"), dupes));
        }

        List<String> entryNames = all.stream().map(JarEntryUtils.Entry::name).toList();
        Set<String> malwareRoots = FakeBstatsMalwareDetector.discoverMalwareRoots(entryNames);

        List<JarEntryUtils.Entry> filtered = new ArrayList<>();
        for (JarEntryUtils.Entry entry : all) {
            if (FakeBstatsMalwareDetector.shouldRemoveEntry(entry.name(), malwareRoots)) {
                b0b0b0Dick.log(String.format(conf.getTranslation("removingFile"), entry.name()));
                continue;
            }
            if (isBrokenSignatureEntry(entry.name())) {
                continue;
            }

            if (!entry.isDirectory() && entry.name().endsWith(".class")) {
                byte[] cleaned = stripMalwareReferences(entry.data(), malwareRoots);
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

    private boolean isBrokenSignatureEntry(String name) {
        return name.startsWith("META-INF/")
                && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA"));
    }

    private byte[] stripMalwareReferences(byte[] clazz, Set<String> malwareRoots) {
        if (!FakeBstatsMalwareDetector.needsBytecodeCleanup(clazz, malwareRoots)) {
            return clazz;
        }

        try {
            ClassReader cr = new ClassReader(clazz);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);

            boolean changed = false;
            for (MethodNode method : cn.methods) {
                if (stripMethodReferences(method, malwareRoots)) {
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

    private boolean stripMethodReferences(MethodNode method, Set<String> malwareRoots) {
        List<AbstractInsnNode> trash = new ArrayList<>();

        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode min
                    && FakeBstatsMalwareDetector.isMalwareInternalName(min.owner, malwareRoots)) {
                trash.add(insn);
                int argCount = Type.getArgumentTypes(min.desc).length;
                if (min.getOpcode() != Opcodes.INVOKESTATIC) {
                    argCount++;
                }
                collectArgumentLoads(trash, insn.getPrevious(), argCount);
                continue;
            }

            if (insn instanceof FieldInsnNode fin
                    && FakeBstatsMalwareDetector.isMalwareInternalName(fin.owner, malwareRoots)) {
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
                    && FakeBstatsMalwareDetector.isMalwareInternalName(tin.desc, malwareRoots)) {
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
}
