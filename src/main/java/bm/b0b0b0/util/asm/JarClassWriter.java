package bm.b0b0b0.util.asm;

import bm.b0b0b0.util.files.JarEntryUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JarClassWriter {

    private JarClassWriter() {
    }

    public static Map<String, byte[]> indexClasses(List<JarEntryUtils.Entry> all) {
        Map<String, byte[]> map = new HashMap<>();
        for (JarEntryUtils.Entry entry : all) {
            if (!entry.isDirectory() && entry.name().endsWith(".class") && entry.data() != null) {
                String internal = entry.name().substring(0, entry.name().length() - ".class".length());
                map.put(entry.name(), entry.data());
                map.put(internal, entry.data());
            }
        }
        return map;
    }

    public static byte[] writePatchedMethods(ClassReader cr, ClassNode cn, Map<String, byte[]> jarClasses,
                                             Set<String> touchedMethodKeys) {
        MethodBodyGuard.ensureClassBodies(cn);
        if (touchedMethodKeys.isEmpty()) {
            return writeFresh(cn, jarClasses);
        }
        finalizeTouchedMethods(cn, touchedMethodKeys);
        stripAllFrames(cn);
        try {
            return emitComputedFrames(cn, jarClasses);
        } catch (Throwable primary) {
            return emitComputed(cloneToReader(cn), cn, jarClasses);
        }
    }

    public static void finalizeTouchedMethods(ClassNode cn, Set<String> touchedMethodKeys) {
        for (MethodNode method : cn.methods) {
            if (!touchedMethodKeys.contains(method.name + method.desc)) {
                continue;
            }
            finalizeMethodForFrameRecompute(cn.name, method);
        }
        TryCatchBlockPruner.prune(cn);
    }

    public static byte[] writeFresh(ClassNode cn, Map<String, byte[]> jarClasses) {
        MethodBodyGuard.ensureClassBodies(cn);
        finalizeAllMethods(cn);
        stripAllFrames(cn);
        try {
            return emitComputedFrames(cn, jarClasses);
        } catch (Throwable primary) {
            ClassWriter cw = new JarAwareClassWriter(
                    ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, jarClasses);
            cn.accept(cw);
            return cw.toByteArray();
        }
    }

    public static byte[] writePreserving(ClassReader cr, ClassNode cn) {
        MethodBodyGuard.ensureClassBodies(cn);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    public static byte[] writeTree(ClassNode cn, Map<String, byte[]> jarClasses) {
        return writeModified(cn, jarClasses);
    }

    public static byte[] writeModified(ClassNode cn, Map<String, byte[]> jarClasses) {
        return writeModified(cn, jarClasses, null);
    }

    public static byte[] writeModified(ClassNode cn, Map<String, byte[]> jarClasses, Set<String> touchedMethodKeys) {
        if (touchedMethodKeys != null && !touchedMethodKeys.isEmpty()) {
            ClassReader cr = new ClassReader(cloneToBytes(cn));
            return writePatchedMethods(cr, cn, jarClasses, touchedMethodKeys);
        }
        ClassReader cr = new ClassReader(cloneToBytes(cn));
        return writePreserving(cr, cn);
    }

    private static byte[] cloneToBytes(ClassNode cn) {
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static ClassReader cloneToReader(ClassNode cn) {
        return new ClassReader(cloneToBytes(cn));
    }

    private static byte[] emitComputedFrames(ClassNode cn, Map<String, byte[]> jarClasses) {
        ClassWriter cw = new JarAwareClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, jarClasses);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static void stripAllFrames(ClassNode cn) {
        for (MethodNode method : cn.methods) {
            FrameRecomputer.stripFrames(method);
        }
    }

    public static byte[] write(ClassReader cr, ClassNode cn, Map<String, byte[]> jarClasses) {
        try {
            ClassNode working = cloneClass(cn);
            finalizeAllMethods(working);
            stripAllFrames(working);
            return emitComputed(cr, working, jarClasses);
        } catch (Throwable primary) {
            try {
                return writeLenient(cr, cn, jarClasses);
            } catch (Throwable lenient) {
                return emitComputed(cr, cn, jarClasses);
            }
        }
    }

    public static byte[] writeMinimal(ClassReader cr, ClassNode cn) {
        ClassNode working = cloneClass(cn);
        finalizeAllMethods(working);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        working.accept(cw);
        return cw.toByteArray();
    }

    public static byte[] writeLenient(ClassReader cr, ClassNode cn, Map<String, byte[]> jarClasses) {
        ClassNode working = cloneClass(cn);
        finalizeAllMethods(working);
        try {
            stripAllFrames(working);
            return emitComputed(cr, working, jarClasses);
        } catch (Throwable ignored) {
            return emitComputed(cr, working, jarClasses);
        }
    }

    public static byte[] writeComputed(ClassReader cr, ClassNode cn, Map<String, byte[]> jarClasses) {
        ClassNode working = cloneClass(cn);
        finalizeAllMethods(working);
        return emitComputed(cr, working, jarClasses);
    }

    public static byte[] emitComputed(ClassReader cr, ClassNode cn, Map<String, byte[]> jarClasses) {
        try {
            ClassWriter cw = new JarAwareClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, jarClasses);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable ignored) {
            ClassWriter cw = new JarAwareClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, jarClasses);
            cn.accept(cw);
            return cw.toByteArray();
        }
    }

    private static void finalizeAllMethods(ClassNode cn) {
        for (MethodNode method : cn.methods) {
            finalizeMethodForFrameRecompute(cn.name, method);
        }
        TryCatchBlockPruner.prune(cn);
    }

    private static void finalizeMethodForFrameRecompute(String owner, MethodNode method) {
        MethodBodyGuard.ensureMethodBody(method);
        TryCatchBlockDeduplicator.dedupe(method);
        DeadTryCatchRemover.cleanMethod(method);
        TryCatchBlockPruner.prune(method);
        OrphanPopRemover.cleanMethod(owner, method);
        DeadTryCatchRemover.repairHandlerJumps(method);
        TryCatchBlockPruner.prune(method);
        FrameRecomputer.stripFrames(method);
        method.localVariables = null;
    }

    private static ClassNode cloneClass(ClassNode cn) {
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        ClassReader reader = new ClassReader(cw.toByteArray());
        ClassNode clone = new ClassNode();
        reader.accept(clone, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return clone;
    }

    private static final class JarAwareClassWriter extends ClassWriter {

        private final VerifyStubClassLoader stubLoader;

        JarAwareClassWriter(ClassReader cr, int flags, Map<String, byte[]> jarClasses) {
            super(cr, flags);
            this.stubLoader = new VerifyStubClassLoader(jarClasses, getClass().getClassLoader());
            this.stubLoader.predefineJarClasses();
        }

        JarAwareClassWriter(int flags, Map<String, byte[]> jarClasses) {
            super(flags);
            this.stubLoader = new VerifyStubClassLoader(jarClasses, getClass().getClassLoader());
            this.stubLoader.predefineJarClasses();
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return stubLoader.commonSuperClass(type1, type2);
        }
    }
}
