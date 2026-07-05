package bm.b0b0b0.util.files;

import bm.b0b0b0.util.asm.JarClassWriter;
import bm.b0b0b0.util.asm.DeadTryCatchRemover;
import bm.b0b0b0.util.asm.OrphanPopRemover;
import bm.b0b0b0.util.asm.TryCatchBlockDeduplicator;
import bm.b0b0b0.util.asm.TryCatchBlockPruner;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class CleanPipeline {

    private static final ThreadLocal<Boolean> UNIFIED = ThreadLocal.withInitial(() -> false);

    private CleanPipeline() {
    }

    public static void beginUnified(File inputDir, File outputDir) {
        UNIFIED.set(true);
        if (outputDir == null) {
            return;
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            return;
        }
        seedOutputFromInput(inputDir, outputDir);
    }

    public static void beginUnified(File outputDir) {
        beginUnified(null, outputDir);
    }

    private static void seedOutputFromInput(File inputDir, File outputDir) {
        if (inputDir == null || !inputDir.isDirectory()) {
            return;
        }
        File[] inputJars = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (inputJars == null) {
            return;
        }
        for (File jar : inputJars) {
            File dest = new File(outputDir, jar.getName());
            try {
                Files.copy(jar.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
        }
    }

    public static void endUnified() {
        UNIFIED.remove();
    }

    public static boolean isUnified() {
        return UNIFIED.get();
    }

    public static File sourceFile(File inputJar, File outputDir) {
        if (!isUnified()) {
            return inputJar;
        }
        File partial = new File(outputDir, inputJar.getName());
        return partial.exists() ? partial : inputJar;
    }

    public static File outputFile(File outputDir, String originalName) {
        if (isUnified()) {
            return new File(outputDir, originalName);
        }
        return new File(outputDir, FileUtils.generateUniqueFileName(outputDir, originalName));
    }

    public static void finalizeOutputJars(File outputDir) {
        if (outputDir == null || !outputDir.isDirectory()) {
            return;
        }
        try (Stream<Path> paths = Files.walk(outputDir.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .forEach(CleanPipeline::finalizeJar);
        } catch (IOException ignored) {
        }
    }

    private static void finalizeJar(Path jarPath) {
        try {
            File jar = jarPath.toFile();
            List<JarEntryUtils.Entry> entries = JarEntryUtils.readDeduped(jar);
            Map<String, byte[]> jarClasses = JarClassWriter.indexClasses(entries);
            List<JarEntryUtils.Entry> finalized = new ArrayList<>(entries.size());
            boolean changed = false;

            for (JarEntryUtils.Entry entry : entries) {
                if (entry.isDirectory() || !entry.name().endsWith(".class") || entry.data() == null) {
                    finalized.add(entry);
                    continue;
                }

                byte[] data = finalizeClass(entry.data(), jarClasses);
                if (data != entry.data()) {
                    changed = true;
                    String internalName = entry.name().substring(0, entry.name().length() - ".class".length());
                    jarClasses.put(entry.name(), data);
                    jarClasses.put(internalName, data);
                }
                finalized.add(new JarEntryUtils.Entry(entry.name(), false, data));
            }

            if (changed) {
                JarEntryUtils.writeJar(jar, finalized);
            }
        } catch (Throwable ignored) {
        }
    }

    private static byte[] finalizeClass(byte[] data, Map<String, byte[]> jarClasses) {
        try {
            ClassReader cr = new ClassReader(data);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            int before = structuralSize(cn);
            boolean riskyFrames = hasFrameRisk(cn);
            for (MethodNode method : cn.methods) {
                TryCatchBlockDeduplicator.dedupe(method);
                DeadTryCatchRemover.cleanMethod(method);
                TryCatchBlockPruner.prune(method);
                OrphanPopRemover.cleanMethod(cn.name, method);
                TryCatchBlockPruner.prune(method);
            }
            TryCatchBlockPruner.prune(cn);
            if (!riskyFrames && structuralSize(cn) == before) {
                return data;
            }
            return JarClassWriter.writeModified(cn, jarClasses);
        } catch (Throwable ignored) {
            return data;
        }
    }

    private static int structuralSize(ClassNode cn) {
        int size = 0;
        for (MethodNode method : cn.methods) {
            size += method.instructions == null ? 0 : method.instructions.size();
            size += method.tryCatchBlocks == null ? 0 : method.tryCatchBlocks.size() * 31;
        }
        return size;
    }

    private static boolean hasFrameRisk(ClassNode cn) {
        for (MethodNode method : cn.methods) {
            if (method.tryCatchBlocks != null) {
                if (method.tryCatchBlocks.size() > 8) {
                    return true;
                }
                for (TryCatchBlockNode block : method.tryCatchBlocks) {
                    if ("org/bukkit/configuration/InvalidConfigurationException".equals(block.type)) {
                        return true;
                    }
                }
            }
            if (hasAddSuppressed(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAddSuppressed(MethodNode method) {
        if (method.instructions == null) {
            return false;
        }
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof MethodInsnNode min
                    && "java/lang/Throwable".equals(min.owner)
                    && "addSuppressed".equals(min.name)
                    && "(Ljava/lang/Throwable;)V".equals(min.desc)) {
                return true;
            }
        }
        return false;
    }
}
