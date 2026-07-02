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
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static bm.b0b0b0.util.files.FileUtils.generateUniqueFileName;

public class ruBstatsHack {

    private static final int SHADED_LIB_MIN_CLASSES = 5;

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

        MalwareJarContext context = buildMalwareContext(file, all);

        List<JarEntryUtils.Entry> filtered = new ArrayList<>();
        for (JarEntryUtils.Entry entry : all) {
            if (shouldRemoveEntry(entry.name(), context)) {
                b0b0b0Dick.log(String.format(conf.getTranslation("removingFile"), entry.name()));
                continue;
            }
            if (isBrokenSignatureEntry(entry.name())) {
                continue;
            }

            if (!entry.isDirectory() && entry.name().endsWith(".class")) {
                byte[] cleaned = stripMalwareReferences(entry.data(), context);
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

    private MalwareJarContext buildMalwareContext(File file, List<JarEntryUtils.Entry> entries) {
        List<String> entryNames = entries.stream().map(JarEntryUtils.Entry::name).toList();
        Set<String> roots = FakeBstatsMalwareDetector.discoverMalwareRoots(entryNames);
        String mainPackagePrefix = readMainPackagePrefix(file);
        String mainClassEntry = readMainClassEntry(file);
        Map<String, Integer> classesPerPrefix = countClassesPerTwoComponentPrefix(entryNames);

        Set<String> extraOwners = new HashSet<>();
        Set<String> extraPrefixes = new HashSet<>();

        for (JarEntryUtils.Entry entry : entries) {
            if (entry.isDirectory() || !entry.name().endsWith(".class")) {
                continue;
            }
            String name = entry.name();
            if (name.startsWith(FakeBstatsMalwareDetector.LEGIT_PREFIX)) {
                continue;
            }

            String internal = name.substring(0, name.length() - ".class".length());
            if (FakeBstatsMalwareDetector.isMarkerEntry(name)
                    || FakeBstatsMalwareDetector.hasMalwareEntryPath(name)) {
                extraOwners.add(internal);
                continue;
            }

            byte[] data = entry.data();
            if (!FakeBstatsMalwareDetector.containsStrongMalwareBytes(data)) {
                continue;
            }

            extraOwners.add(internal);
            String dropPrefix = pickDropPrefix(name, classesPerPrefix, mainPackagePrefix);
            if (dropPrefix != null) {
                extraPrefixes.add(dropPrefix);
            }
        }

        return new MalwareJarContext(roots, extraOwners, extraPrefixes, mainPackagePrefix, mainClassEntry);
    }

    private boolean shouldRemoveEntry(String name, MalwareJarContext context) {
        if (context.mainClassEntry() != null && name.equals(context.mainClassEntry())) {
            return false;
        }
        if (isProtectedMainPackageEntry(name, context)) {
            return false;
        }
        if (FakeBstatsMalwareDetector.shouldRemoveEntry(name, context.roots())) {
            return true;
        }
        for (String prefix : context.extraPrefixes()) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        if (name.endsWith(".class")) {
            String internal = name.substring(0, name.length() - ".class".length());
            if (context.extraOwners().contains(internal)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProtectedMainPackageEntry(String entryName, MalwareJarContext context) {
        String mainPrefix = context.mainPackagePrefix();
        if (mainPrefix == null || !entryName.startsWith(mainPrefix)) {
            return false;
        }
        for (String root : context.roots()) {
            if (entryName.startsWith(root)) {
                return false;
            }
        }
        for (String prefix : context.extraPrefixes()) {
            if (entryName.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private static String readMainPackagePrefix(File jarFile) {
        String mainClass = readMainClass(jarFile);
        if (mainClass == null) {
            return null;
        }
        int lastDot = mainClass.lastIndexOf('.');
        if (lastDot <= 0) {
            return null;
        }
        return mainClass.substring(0, lastDot).replace('.', '/') + "/";
    }

    private static String readMainClassEntry(File jarFile) {
        String mainClass = readMainClass(jarFile);
        if (mainClass == null) {
            return null;
        }
        return mainClass.replace('.', '/') + ".class";
    }

    private static String readMainClass(File jarFile) {
        String[] candidates = {"plugin.yml", "bungee.yml", "paper-plugin.yml", "velocity-plugin.json"};
        try (ZipFile zip = new ZipFile(jarFile)) {
            for (String candidate : candidates) {
                ZipEntry entry = zip.getEntry(candidate);
                if (entry == null) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    Matcher matcher = Pattern.compile("[\"']?main[\"']?\\s*[:=]\\s*[\"']?([\\w.$]+)[\"']?")
                            .matcher(text);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Map<String, Integer> countClassesPerTwoComponentPrefix(List<String> entryNames) {
        Map<String, Integer> counts = new HashMap<>();
        for (String name : entryNames) {
            if (!name.endsWith(".class") || name.startsWith(FakeBstatsMalwareDetector.LEGIT_PREFIX)) {
                continue;
            }
            String prefix = twoComponentPrefix(name);
            if (prefix != null) {
                counts.merge(prefix, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static String pickDropPrefix(String entryName,
                                         Map<String, Integer> classesPerPrefix,
                                         String mainPackagePrefix) {
        int bstatsIdx = entryName.indexOf(FakeBstatsMalwareDetector.BSTATS_SEGMENT);
        String candidate = bstatsIdx > 0
                ? entryName.substring(0, bstatsIdx + FakeBstatsMalwareDetector.BSTATS_SEGMENT.length())
                : twoComponentPrefix(entryName);
        if (candidate == null || isSafePrefix(candidate)) {
            return null;
        }
        if (mainPackagePrefix != null
                && (candidate.equals(mainPackagePrefix) || mainPackagePrefix.startsWith(candidate))) {
            return null;
        }
        if (countClassesUnder(classesPerPrefix, candidate) < SHADED_LIB_MIN_CLASSES) {
            return null;
        }
        return candidate;
    }

    private static int countClassesUnder(Map<String, Integer> classesPerPrefix, String candidate) {
        Integer direct = classesPerPrefix.get(candidate);
        if (direct != null && candidate.chars().filter(ch -> ch == '/').count() == 2) {
            return direct;
        }
        int firstSlash = candidate.indexOf('/');
        int secondSlash = firstSlash < 0 ? -1 : candidate.indexOf('/', firstSlash + 1);
        if (secondSlash > 0) {
            Integer parent = classesPerPrefix.get(candidate.substring(0, secondSlash + 1));
            if (parent != null) {
                return parent;
            }
        }
        return 0;
    }

    private static String twoComponentPrefix(String entryName) {
        int first = entryName.indexOf('/');
        if (first <= 0) {
            return null;
        }
        int second = entryName.indexOf('/', first + 1);
        if (second <= first + 1) {
            return null;
        }
        return entryName.substring(0, second + 1);
    }

    private static boolean isSafePrefix(String prefix) {
        return prefix.equals(FakeBstatsMalwareDetector.LEGIT_PREFIX)
                || prefix.equals("org/objectweb/")
                || prefix.equals("META-INF/");
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

    private byte[] stripMalwareReferences(byte[] clazz, MalwareJarContext context) {
        if (!FakeBstatsMalwareDetector.needsBytecodeCleanup(
                clazz, context.roots(), context.extraOwners(), context.extraPrefixes())) {
            return clazz;
        }

        try {
            ClassReader cr = new ClassReader(clazz);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);

            if (cn.innerClasses != null) {
                cn.innerClasses.removeIf(ic -> isMalwareReference(ic.name, context)
                        || isMalwareReference(ic.outerName, context));
            }

            for (MethodNode method : cn.methods) {
                stripMethodReferences(method, context);
                if (method.tryCatchBlocks != null) {
                    for (TryCatchBlockNode block : method.tryCatchBlocks) {
                        if (block.type != null && isMalwareReference(block.type, context)) {
                            block.type = "java/lang/Throwable";
                        }
                    }
                }
            }

            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable ignored) {
            return clazz;
        }
    }

    private boolean stripMethodReferences(MethodNode method, MalwareJarContext context) {
        List<AbstractInsnNode> trash = new ArrayList<>();
        boolean rewroteInPlace = false;

        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode min && isMalwareReference(min.owner, context)) {
                trash.add(insn);
                int argCount = Type.getArgumentTypes(min.desc).length;
                if (min.getOpcode() != Opcodes.INVOKESTATIC) {
                    argCount++;
                }
                collectArgumentLoads(trash, insn.getPrevious(), argCount);
                continue;
            }

            if (insn instanceof FieldInsnNode fin && isMalwareReference(fin.owner, context)) {
                trash.add(insn);
                if (fin.getOpcode() != Opcodes.GETSTATIC && fin.getOpcode() != Opcodes.PUTSTATIC) {
                    AbstractInsnNode prev = insn.getPrevious();
                    if (prev != null) {
                        trash.add(prev);
                    }
                }
                continue;
            }

            if (insn instanceof TypeInsnNode tin && isMalwareReference(tin.desc, context)) {
                if (tin.getOpcode() == Opcodes.NEW) {
                    trash.add(insn);
                    for (AbstractInsnNode cur = insn.getNext(); cur != null; cur = cur.getNext()) {
                        trash.add(cur);
                        int op = cur.getOpcode();
                        if (op == Opcodes.POP || op == Opcodes.POP2 || op == Opcodes.ASTORE || op == Opcodes.INVOKESPECIAL) {
                            break;
                        }
                    }
                } else if (tin.getOpcode() == Opcodes.CHECKCAST || tin.getOpcode() == Opcodes.INSTANCEOF) {
                    tin.desc = "java/lang/Object";
                    rewroteInPlace = true;
                }
                continue;
            }

            if (insn instanceof LdcInsnNode ldc) {
                if (ldc.cst instanceof String string && FakeBstatsMalwareDetector.stringContainsMalwareMarker(string)) {
                    ldc.cst = "";
                    rewroteInPlace = true;
                    continue;
                }
                if (ldc.cst instanceof Type type && isMalwareReference(type.getInternalName(), context)) {
                    ldc.cst = Type.getType("Ljava/lang/Object;");
                    rewroteInPlace = true;
                }
            }
        }

        if (trash.isEmpty() && !rewroteInPlace) {
            return false;
        }

        trash.forEach(method.instructions::remove);
        return true;
    }

    private static boolean isMalwareReference(String internalName, MalwareJarContext context) {
        return FakeBstatsMalwareDetector.isMalwareInternalName(
                internalName, context.roots(), context.extraOwners(), context.extraPrefixes());
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

    private record MalwareJarContext(
            Set<String> roots,
            Set<String> extraOwners,
            Set<String> extraPrefixes,
            String mainPackagePrefix,
            String mainClassEntry
    ) {
    }
}
