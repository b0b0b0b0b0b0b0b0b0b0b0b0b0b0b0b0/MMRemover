package bm.b0b0b0.hacks;

import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.asm.CaseInsensitiveClassRefFixer;
import bm.b0b0b0.util.asm.CatchHandlerLocalFixer;
import bm.b0b0b0.util.asm.DeadTryCatchRemover;
import bm.b0b0b0.util.asm.JarClassWriter;
import bm.b0b0b0.util.asm.OrphanPopRemover;
import bm.b0b0b0.util.asm.TryCatchBlockPruner;
import bm.b0b0b0.util.files.CleanPipeline;
import bm.b0b0b0.util.files.JarEntryUtils;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class chbkHack {

    private static final String[] BAD_CLASS_PREFIXES = {
            "org/apache/commons/lang3/packet/",
    };

    private static final String[] CHAMBOR_LANG3_CLASSES = {
            "org/apache/commons/lang3/AccessMake",
    };

    private static final String[] CHAMBOR_TEXT_PREFIXES = {
            "org/apache/commons/lang3/text/TextChatComponent",
            "org/apache/commons/lang3/text/ChatEncoders",
            "org/apache/commons/lang3/text/RuntimeEnv",
    };

    private static final String MUTABLE_UTILITIES = "org/apache/commons/lang3/MutableUtilities";
    private static final String MUTABLE_UTILITIES_CLASS = "org/apache/commons/lang3/MutableUtilities.class";
    private static final byte[] CHAMBOR_URL = "http://basher.ru".getBytes(StandardCharsets.UTF_8);

    private final List<String> infectedFiles = new CopyOnWriteArrayList<>();
    private final Path outputPath;
    private final ExecutorService executor;
    private final Conf conf;

    public static void cleanJar(File src, File outputDir) throws IOException {
        cleanJar(src, outputDir, new Conf());
    }

    public static void cleanJar(File src, File outputDir, Conf conf) throws IOException {
        new chbkHack(conf, outputDir.toPath()).runRemover(src, outputDir);
    }

    private chbkHack(Conf conf, Path outputPath) {
        this.conf = conf;
        this.outputPath = outputPath;
        this.executor = null;
    }

    public chbkHack(Path pluginsPath, Path outputPath,
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

            for (ZipEntry entry : zip.stream().toList()) {
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                byte[] data = zip.getInputStream(entry).readAllBytes();
                if (indexOf(data, CHAMBOR_URL) != -1) {
                    tagInfected(file, String.format(conf.getTranslation("infectionMutableString"), entry.getName()));
                    return;
                }
                if (indexOf(data, "MutableUtilities".getBytes(StandardCharsets.UTF_8)) == -1) {
                    continue;
                }
                try {
                    ClassReader cr = new ClassReader(data);
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    if (referencesMutableUtilities(cn)) {
                        tagInfected(file, String.format(conf.getTranslation("infectionMutableCalls"), entry.getName()));
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
        b0b0b0Dick.log(reason + " (" + file.getName() + ")");
    }

    private void runRemover(File file, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException(String.format(conf.getTranslation("outputDirCreateFailed"), outputDir));
        }

        File src = CleanPipeline.sourceFile(file, outputDir);
        List<JarEntryUtils.Entry> all = JarEntryUtils.readDeduped(src);
        boolean jarInfected = isJarInfected(all);

        int totalClasses = 0;
        int payloadClasses = 0;
        List<String> patchTargets = new ArrayList<>();
        Set<String> patchSet = new HashSet<>();
        for (JarEntryUtils.Entry entry : all) {
            if (entry.isDirectory() || !entry.name().endsWith(".class")) {
                continue;
            }
            totalClasses++;
            if (shouldSkipClass(entry.name(), jarInfected)) {
                payloadClasses++;
                continue;
            }
            if (mustPatchClass(entry.data())) {
                patchTargets.add(entry.name());
                patchSet.add(entry.name());
            }
        }

        long bytecodePhaseStart = System.nanoTime();
        b0b0b0Dick.log(String.format(conf.getTranslation("jarBytecodeStart"),
                totalClasses, payloadClasses, patchTargets.size()));

        Map<String, byte[]> jarClasses = patchTargets.isEmpty()
                ? Map.of()
                : JarClassWriter.indexClasses(all);

        int patched = 0;
        int removedPayload = 0;
        List<JarEntryUtils.Entry> filtered = new ArrayList<>(all.size());
        for (JarEntryUtils.Entry entry : all) {
            if (!entry.isDirectory() && entry.name().endsWith(".class")) {
                if (shouldSkipClass(entry.name(), jarInfected)) {
                    removedPayload++;
                    continue;
                }

                byte[] out = entry.data();
                if (patchSet.contains(entry.name())) {
                    out = cleanClass(entry, jarClasses);
                    patched++;
                    b0b0b0Dick.log(String.format(conf.getTranslation("jarBytecodeProgress"),
                            patched, patchTargets.size(), patched, entry.name()));

                    String internal = entry.name().substring(0, entry.name().length() - 6);
                    jarClasses.put(entry.name(), out);
                    jarClasses.put(internal, out);
                }
                filtered.add(new JarEntryUtils.Entry(entry.name(), false, out));
            } else {
                filtered.add(entry);
            }
        }

        if (removedPayload > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("jarPayloadRemoved"), removedPayload));
        }
        CaseInsensitiveClassRefFixer.Context caseContext = CaseInsensitiveClassRefFixer.Context.of(
                filtered.stream().filter(e -> !e.isDirectory() && e.name().endsWith(".class")).map(JarEntryUtils.Entry::name).toList());
        Map<String, byte[]> jarClassesForCaseFix = JarClassWriter.indexClasses(filtered);
        List<JarEntryUtils.Entry> finalized = new ArrayList<>(filtered.size());
        for (JarEntryUtils.Entry entry : filtered) {
            if (entry.isDirectory() || !entry.name().endsWith(".class") || entry.data() == null) {
                finalized.add(entry);
                continue;
            }
            byte[] fixed = CaseInsensitiveClassRefFixer.fix(entry.data(), caseContext, jarClassesForCaseFix);
            finalized.add(new JarEntryUtils.Entry(entry.name(), false, fixed));
        }
        filtered = finalized;
        double bytecodeSec = (System.nanoTime() - bytecodePhaseStart) / 1_000_000_000.0;
        b0b0b0Dick.log(String.format(conf.getTranslation("jarBytecodeDone"), bytecodeSec));

        File outFile = CleanPipeline.outputFile(outputDir, file.getName());
        b0b0b0Dick.log(String.format(conf.getTranslation("creatingCleanFile"), outFile.getAbsolutePath()));
        JarEntryUtils.writeJar(outFile, filtered);

        if (outFile.length() > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("cleanFileCreatedSuccessfully"), outFile.getAbsolutePath()));
        } else {
            b0b0b0Dick.log(String.format(conf.getTranslation("failedToCreateCleanFile"), outFile.getAbsolutePath()));
        }
    }

    private static boolean isJarInfected(List<JarEntryUtils.Entry> all) {
        for (JarEntryUtils.Entry entry : all) {
            if (!entry.isDirectory() && isMutableUtilitiesClass(entry.name())) {
                return true;
            }
        }
        return false;
    }

    public static boolean classReferencesMutableUtilities(byte[] clazz) {
        try {
            ClassReader cr = new ClassReader(clazz);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return referencesMutableUtilities(cn);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private byte[] cleanClass(JarEntryUtils.Entry entry, Map<String, byte[]> jarClasses) {
        try {
            return patchMutableUtilitiesCalls(entry.data(), jarClasses);
        } catch (Throwable ex) {
            b0b0b0Dick.log(String.format(conf.getTranslation("skipClassWarning"), entry.name(), ex.getMessage()));
            return entry.data();
        }
    }

    private static boolean mustPatchClass(byte[] clazz) {
        if (clazz == null || clazz.length == 0 || !containsMutableUtilitiesString(clazz)) {
            return false;
        }
        try {
            ClassReader cr = new ClassReader(clazz);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return referencesMutableUtilities(cn);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private byte[] patchMutableUtilitiesCalls(byte[] clazz, Map<String, byte[]> jarClasses) {
        ClassReader cr = new ClassReader(clazz);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);

        boolean hasRefs = referencesMutableUtilities(cn);
        boolean changed = false;
        Set<String> touchedMethods = new HashSet<>();
        for (MethodNode m : cn.methods) {
            boolean methodChanged = false;
            if (hasRefs) {
                while (stripCalls(m)) {
                    changed = true;
                    methodChanged = true;
                }
            }
            if (methodChanged) {
                DeadTryCatchRemover.cleanMethod(m);
            }
            if (shouldStripChamborMethod(m, hasRefs, methodChanged) && stripChamborCatchAll(m)) {
                changed = true;
                methodChanged = true;
            }
            if (methodChanged) {
                DeadTryCatchRemover.repairHandlerJumps(m);
            }
            if (methodChanged) {
                while (OrphanPopRemover.cleanMethod(cn.name, m)) {
                    TryCatchBlockPruner.prune(m);
                }
            }
            if (methodChanged) {
                touchedMethods.add(m.name + m.desc);
            }
        }

        if (hasRefs && changed) {
            for (MethodNode m : cn.methods) {
                String key = m.name + m.desc;
                if (!touchedMethods.contains(key) && CatchHandlerLocalFixer.needsFix(m, 800)) {
                    touchedMethods.add(key);
                }
            }
        }

        if (!changed) {
            return clazz;
        }
        return rewriteClassOrFallback(cr, cn, jarClasses, clazz, touchedMethods);
    }

    private byte[] rewriteClassOrFallback(ClassReader cr, ClassNode cn, Map<String, byte[]> jarClasses,
                                          byte[] fallback, Set<String> touchedMethods) {
        try {
            return JarClassWriter.writeModified(cn, jarClasses, touchedMethods);
        } catch (Throwable primary) {
            try {
                return JarClassWriter.writePatchedMethods(cr, cn, jarClasses, touchedMethods);
            } catch (Throwable secondary) {
                try {
                    return JarClassWriter.emitComputed(cr, cn, jarClasses);
                } catch (Throwable tertiary) {
                    b0b0b0Dick.log(String.format(conf.getTranslation("skipClassWarning"),
                            cn.name + ".class", skipReason(tertiary)));
                    return fallback;
                }
            }
        }
    }

    private static String skipReason(Throwable ex) {
        String message = ex.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return ex.getClass().getSimpleName();
    }

    private static boolean containsMutableUtilitiesString(byte[] clazz) {
        if (clazz == null || clazz.length == 0) {
            return false;
        }
        String s = new String(clazz, StandardCharsets.ISO_8859_1);
        return s.contains(MUTABLE_UTILITIES) || s.contains("MutableUtilities");
    }

    private static boolean shouldSkipClass(String name, boolean jarInfected) {
        if (!name.endsWith(".class")) {
            return false;
        }
        if (isMutableUtilitiesClass(name)) {
            return true;
        }
        for (String prefix : CHAMBOR_TEXT_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        for (String prefix : CHAMBOR_LANG3_CLASSES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        for (String prefix : BAD_CLASS_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMutableUtilitiesClass(String name) {
        return MUTABLE_UTILITIES_CLASS.equals(name)
                || name.startsWith("org/apache/commons/lang3/MutableUtilities$");
    }

    private static boolean isMutableUtilitiesType(String internal) {
        return MUTABLE_UTILITIES.equals(internal)
                || (internal != null && internal.startsWith(MUTABLE_UTILITIES + "$"));
    }

    private static boolean referencesMutableUtilities(ClassNode cn) {
        for (MethodNode m : cn.methods) {
            for (AbstractInsnNode ins : m.instructions) {
                if (ins instanceof MethodInsnNode min && isMutableUtilitiesType(min.owner)) {
                    return true;
                }
                if (ins instanceof FieldInsnNode fin && isMutableUtilitiesType(fin.owner)) {
                    return true;
                }
                if (ins instanceof TypeInsnNode t && isMutableUtilitiesType(t.desc)
                        && (t.getOpcode() == Opcodes.NEW || t.getOpcode() == Opcodes.INSTANCEOF
                        || t.getOpcode() == Opcodes.CHECKCAST)) {
                    return true;
                }
                if (ins instanceof LdcInsnNode ldc && ldc.cst instanceof Type type
                        && isMutableUtilitiesType(type.getInternalName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean methodReferencesMutableUtilities(MethodNode m) {
        for (AbstractInsnNode ins : m.instructions) {
            if (ins instanceof MethodInsnNode min && isMutableUtilitiesType(min.owner)) {
                return true;
            }
            if (ins instanceof FieldInsnNode fin && isMutableUtilitiesType(fin.owner)) {
                return true;
            }
            if (ins instanceof TypeInsnNode t && isMutableUtilitiesType(t.desc)
                    && (t.getOpcode() == Opcodes.NEW || t.getOpcode() == Opcodes.INSTANCEOF
                    || t.getOpcode() == Opcodes.CHECKCAST)) {
                return true;
            }
            if (ins instanceof LdcInsnNode ldc && ldc.cst instanceof Type type
                    && isMutableUtilitiesType(type.getInternalName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSystemMetricsMethod(MethodNode m) {
        return "SystemMetrics".equals(m.name) || m.name.startsWith("lambda$SystemMetrics");
    }

    private static boolean shouldStripChamborMethod(MethodNode m, boolean classInfected, boolean methodChanged) {
        if (methodReferencesMutableUtilities(m) || isSystemMetricsMethod(m)) {
            return true;
        }
        if (!classInfected || m.tryCatchBlocks == null || m.tryCatchBlocks.isEmpty()) {
            return false;
        }
        boolean hasBroadCatch = false;
        for (TryCatchBlockNode block : m.tryCatchBlocks) {
            if (block.type != null
                    && !"java/lang/Throwable".equals(block.type)
                    && !"java/lang/Exception".equals(block.type)
                    && !"java/lang/Error".equals(block.type)) {
                continue;
            }
            hasBroadCatch = true;
            if (!DeadTryCatchRemover.isMalwareCatch(m, block)) {
                return false;
            }
        }
        return hasBroadCatch;
    }

    private static boolean stripChamborCatchAll(MethodNode m) {
        if (m.tryCatchBlocks == null || m.tryCatchBlocks.isEmpty()) {
            return false;
        }
        boolean changed = false;
        Iterator<TryCatchBlockNode> it = m.tryCatchBlocks.iterator();
        while (it.hasNext()) {
            TryCatchBlockNode block = it.next();
            if (!DeadTryCatchRemover.isMalwareCatch(m, block)) {
                continue;
            }
            DeadTryCatchRemover.dropHandlerBlock(m, block);
            it.remove();
            changed = true;
        }
        return changed;
    }

    private static boolean stripCalls(MethodNode m) {
        List<AbstractInsnNode> trash = new ArrayList<>();
        Set<Integer> maybeDeadLocals = new HashSet<>();
        Set<AbstractInsnNode> scheduled = new HashSet<>();
        boolean changed = false;

        for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
            if (scheduled.contains(n)) {
                continue;
            }

            if (n instanceof LdcInsnNode ldc && ldc.cst instanceof Type type
                    && isMutableUtilitiesType(type.getInternalName())) {
                ldc.cst = Type.getType("Ljava/lang/Object;");
                changed = true;
                continue;
            }

            if (n instanceof TypeInsnNode t && isMutableUtilitiesType(t.desc)) {
                int op = n.getOpcode();
                if (op == Opcodes.CHECKCAST) {
                    scheduleInsn(trash, scheduled, n);
                    changed = true;
                    continue;
                }
                if (op == Opcodes.INSTANCEOF) {
                    InsnList repl = new InsnList();
                    repl.add(new InsnNode(Opcodes.POP));
                    repl.add(new InsnNode(Opcodes.ICONST_0));
                    m.instructions.insert(n, repl);
                    scheduleInsn(trash, scheduled, n);
                    changed = true;
                    continue;
                }
            }

            if (n instanceof FieldInsnNode fin && isMutableUtilitiesType(fin.owner)) {
                scheduleInsn(trash, scheduled, n);
                if (fin.getOpcode() != Opcodes.PUTSTATIC && fin.getOpcode() != Opcodes.PUTFIELD) {
                    collectArgumentLoads(trash, scheduled, realPrevious(n), 1);
                }
                changed = true;
                continue;
            }

            if (n instanceof MethodInsnNode min && isMutableUtilitiesType(min.owner)) {
                scheduleInsn(trash, scheduled, n);
                int argCount = Type.getArgumentTypes(min.desc).length;
                if (min.getOpcode() != Opcodes.INVOKESTATIC) {
                    argCount++;
                }
                collectArgumentLoads(trash, scheduled, realPrevious(n), argCount);
                AbstractInsnNode prev = realPrevious(n);
                if (prev != null && prev.getOpcode() == Opcodes.NEW
                        && prev instanceof TypeInsnNode prevType
                        && isMutableUtilitiesType(prevType.desc)) {
                    scheduleInsn(trash, scheduled, prev);
                }
                if (!Type.VOID_TYPE.equals(Type.getReturnType(min.desc))) {
                    AbstractInsnNode after = realNext(n);
                    if (after != null
                            && (after.getOpcode() == Opcodes.POP || after.getOpcode() == Opcodes.POP2)) {
                        scheduleInsn(trash, scheduled, after);
                    }
                }
                changed = true;
                continue;
            }

            if (n.getOpcode() == Opcodes.NEW
                    && n instanceof TypeInsnNode t
                    && isMutableUtilitiesType(t.desc)) {
                changed = true;
                AbstractInsnNode prev = realPrevious(n);
                if (prev != null && isAstore(prev)) {
                    maybeDeadLocals.add(((VarInsnNode) prev).var);
                    scheduleInsn(trash, scheduled, prev);
                    AbstractInsnNode prev2 = realPrevious(prev);
                    if (prev2 != null && prev2.getOpcode() == Opcodes.ACONST_NULL) {
                        scheduleInsn(trash, scheduled, prev2);
                    }
                }
                boolean done = false;
                for (AbstractInsnNode cur = n; cur != null && !done; cur = cur.getNext()) {
                    scheduleInsn(trash, scheduled, cur);
                    int op = cur.getOpcode();
                    if (op == Opcodes.POP || op == Opcodes.POP2) {
                        done = true;
                        continue;
                    }
                    if (op == Opcodes.RETURN || op == Opcodes.ARETURN) {
                        done = true;
                        continue;
                    }
                    if (cur instanceof MethodInsnNode call && isMutableUtilitiesType(call.owner)) {
                        if (call.getOpcode() == Opcodes.INVOKESPECIAL) {
                            AbstractInsnNode after = realNext(cur);
                            if (after != null
                                    && (after.getOpcode() == Opcodes.POP || after.getOpcode() == Opcodes.POP2)) {
                                scheduleInsn(trash, scheduled, after);
                            } else if (after != null
                                    && after.getOpcode() != Opcodes.RETURN
                                    && after.getOpcode() != Opcodes.ARETURN) {
                                continue;
                            }
                        }
                        done = true;
                        continue;
                    }
                    if (op == Opcodes.ASTORE) {
                        maybeDeadLocals.add(((VarInsnNode) cur).var);
                        done = true;
                    }
                }
            }
        }

        if (!changed) {
            return false;
        }

        if (!trash.isEmpty()) {
            trash.forEach(m.instructions::remove);
        }

        boolean swept;
        do {
            swept = stripRemainingMuAllocations(m);
            if (swept) {
                changed = true;
            }
        } while (swept);

        if (m.localVariables != null && !maybeDeadLocals.isEmpty()) {
            Set<Integer> used = new HashSet<>();
            for (AbstractInsnNode q = m.instructions.getFirst(); q != null; q = q.getNext()) {
                if (q instanceof VarInsnNode v) {
                    used.add(v.var);
                }
            }
            m.localVariables.removeIf(lv -> maybeDeadLocals.contains(lv.index) && !used.contains(lv.index));
        }
        return true;
    }

    private static boolean stripRemainingMuAllocations(MethodNode m) {
        boolean changed = false;
        for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
            if (n.getOpcode() != Opcodes.NEW || !(n instanceof TypeInsnNode t) || !isMutableUtilitiesType(t.desc)) {
                continue;
            }
            List<AbstractInsnNode> block = new ArrayList<>();
            boolean done = false;
            for (AbstractInsnNode cur = n; cur != null && !done; cur = cur.getNext()) {
                if (cur instanceof LabelNode || cur instanceof LineNumberNode || cur instanceof FrameNode) {
                    continue;
                }
                block.add(cur);
                int op = cur.getOpcode();
                if (op == Opcodes.POP || op == Opcodes.POP2 || op == Opcodes.RETURN || op == Opcodes.ARETURN) {
                    done = true;
                } else if (cur instanceof MethodInsnNode call && isMutableUtilitiesType(call.owner)) {
                    done = true;
                } else if (op == Opcodes.GOTO) {
                    done = true;
                }
            }
            block.forEach(m.instructions::remove);
            changed = true;
        }
        return changed;
    }

    private static void scheduleInsn(List<AbstractInsnNode> trash, Set<AbstractInsnNode> scheduled, AbstractInsnNode insn) {
        if (insn != null && scheduled.add(insn)) {
            trash.add(insn);
        }
    }

    private static void collectArgumentLoads(List<AbstractInsnNode> trash,
                                             Set<AbstractInsnNode> scheduled,
                                             AbstractInsnNode start,
                                             int count) {
        AbstractInsnNode cur = start;
        for (int i = 0; i < count && cur != null; i++) {
            scheduleInsn(trash, scheduled, cur);
            cur = realPrevious(cur);
        }
    }

    private static AbstractInsnNode realPrevious(AbstractInsnNode n) {
        AbstractInsnNode p = n.getPrevious();
        while (p != null && (p instanceof LabelNode || p instanceof FrameNode || p instanceof LineNumberNode)) {
            p = p.getPrevious();
        }
        return p;
    }

    private static AbstractInsnNode realNext(AbstractInsnNode n) {
        AbstractInsnNode next = n.getNext();
        while (next != null && (next instanceof LabelNode || next instanceof FrameNode || next instanceof LineNumberNode)) {
            next = next.getNext();
        }
        return next;
    }

    private static boolean isAstore(AbstractInsnNode insn) {
        return insn.getOpcode() == Opcodes.ASTORE;
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
