package bm.b0b0b0.hacks;

import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static bm.b0b0b0.util.files.FileUtils.generateUniqueFileName;

public class ruBstatsHack {

    private static final String MALWARE_PREFIX = "ru/bstats/";
    private static final byte[] MALWARE_REPORT_URL = "api-bstats.online/api/v2/data/%s".getBytes(StandardCharsets.UTF_8);
    private static final byte[] MALWARE_DOMAIN = "api-bstats.online".getBytes(StandardCharsets.UTF_8);

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

                try (InputStream in = zip.getInputStream(entry)) {
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
        try {
            ClassReader cr = new ClassReader(clazz);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

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

        List<ZipEntryData> all = new ArrayList<>();
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                byte[] data = e.isDirectory() ? null : zip.getInputStream(e).readAllBytes();
                all.add(new ZipEntryData(e.getName(), e.isDirectory(), data));
            }
        }

        List<ZipEntryData> filtered = new ArrayList<>();
        for (ZipEntryData zd : all) {
            if (shouldSkipEntry(zd.name)) {
                b0b0b0Dick.log(String.format(conf.getTranslation("removingFile"), zd.name));
                continue;
            }
            if (isBrokenSignatureEntry(zd.name)) {
                continue;
            }

            if (!zd.isDirectory && zd.name.endsWith(".class")) {
                byte[] cleaned;
                try {
                    cleaned = stripRuBstatsReferences(zd.data);
                } catch (Throwable ex) {
                    b0b0b0Dick.log(String.format(conf.getTranslation("errorProcessingClass"), zd.name));
                    cleaned = zd.data;
                }
                filtered.add(new ZipEntryData(zd.name, false, cleaned));
            } else {
                filtered.add(zd);
            }
        }

        List<ZipEntryData> finalList = removeEmptyDirectories(filtered);

        File outFile = new File(outputDir, generateUniqueFileName(outputDir, file.getName()));
        b0b0b0Dick.log(String.format(conf.getTranslation("creatingCleanFile"), outFile.getAbsolutePath()));

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFile))) {
            for (ZipEntryData z : finalList) {
                out.putNextEntry(new ZipEntry(z.name));
                if (!z.isDirectory) {
                    out.write(z.data);
                }
                out.closeEntry();
            }
        }

        if (outFile.length() > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("cleanFileCreatedSuccessfully"), outFile.getAbsolutePath()));
        } else {
            b0b0b0Dick.log(String.format(conf.getTranslation("failedToCreateCleanFile"), outFile.getAbsolutePath()));
        }
    }

    private boolean shouldSkipEntry(String name) {
        return name.startsWith(MALWARE_PREFIX);
    }

    private boolean isBrokenSignatureEntry(String name) {
        return name.startsWith("META-INF/")
                && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA"));
    }

    private byte[] stripRuBstatsReferences(byte[] clazz) {
        ClassReader cr = new ClassReader(clazz);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        for (MethodNode method : cn.methods) {
            stripMethodReferences(method);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private void stripMethodReferences(MethodNode method) {
        List<AbstractInsnNode> trash = new ArrayList<>();

        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode min && min.owner.startsWith("ru/bstats")) {
                trash.add(insn);
                collectArgumentLoads(trash, insn.getPrevious(), Type.getArgumentTypes(min.desc).length);
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

        trash.forEach(method.instructions::remove);
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

    private List<ZipEntryData> removeEmptyDirectories(List<ZipEntryData> list) {
        Set<String> nonEmpty = new HashSet<>();
        for (ZipEntryData z : list) {
            if (!z.isDirectory) {
                String path = z.name;
                for (int idx = path.lastIndexOf('/'); idx != -1; idx = path.lastIndexOf('/', idx - 1)) {
                    nonEmpty.add(path.substring(0, idx + 1));
                }
            }
        }
        List<ZipEntryData> res = new ArrayList<>();
        for (ZipEntryData z : list) {
            if (!z.isDirectory || nonEmpty.contains(z.name)) {
                res.add(z);
            }
        }
        return res;
    }

    private static int indexOf(byte[] hay, byte[] needle) {
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

    private record ZipEntryData(String name, boolean isDirectory, byte[] data) {}
}
