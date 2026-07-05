package bm.b0b0b0.util.dev;

import bm.b0b0b0.util.asm.*;
import bm.b0b0b0.util.files.JarEntryUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;

public final class DebugSanitizeSteps {
    public static void main(String[] args) throws Exception {
        String jarPath = args.length > 0 ? args[0] : "input/Multiverse-Core-4.3.1.jar";
        String className = args.length > 1 ? args[1] : "com/onarandombox/MultiverseCore/MultiverseCore.class";

        byte[] raw;
        Map<String, byte[]> jarClasses = new HashMap<>();
        try (ZipFile z = new ZipFile(Path.of(jarPath).toFile())) {
            var entries = z.entries();
            while (entries.hasMoreElements()) {
                var e = entries.nextElement();
                if (!e.isDirectory() && e.getName().endsWith(".class")) {
                    byte[] data = z.getInputStream(e).readAllBytes();
                    jarClasses.put(e.getName(), data);
                    jarClasses.put(e.getName().replace(".class", ""), data);
                }
            }
            raw = jarClasses.get(className);
        }

        ClassReader cr = new ClassReader(raw);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        for (MethodNode m : cn.methods) {
            stripMutable(m);
        }

        System.out.println("After strip, try write raw...");
        tryWrite(cr, cn, jarClasses, "strip-only");

        ClassNode base = clone(cn);
        step(base, cr, jarClasses, "1 DeadTryCatch", c -> DeadTryCatchRemover.clean(c));
        step(base, cr, jarClasses, "2 Dedupe", c -> TryCatchBlockDeduplicator.dedupe(c));
        step(base, cr, jarClasses, "3 Prune", c -> TryCatchBlockPruner.prune(c));
        step(base, cr, jarClasses, "4 CatchFix", c -> CatchHandlerLocalFixer.fix(c));
        step(base, cr, jarClasses, "5 OrphanPop", c -> OrphanPopRemover.clean(c));
        step(base, cr, jarClasses, "6 DeadTryCatch2", c -> DeadTryCatchRemover.clean(c));
        step(base, cr, jarClasses, "7 Dedupe2", c -> TryCatchBlockDeduplicator.dedupe(c));
        step(base, cr, jarClasses, "8 Prune2", c -> TryCatchBlockPruner.prune(c));
        step(base, cr, jarClasses, "9 CatchFix2", c -> CatchHandlerLocalFixer.fix(c));
        step(base, cr, jarClasses, "10 full sanitize", c -> BytecodeSanitizer.sanitize(c));
    }

    private static void step(ClassNode cn, ClassReader cr, Map<String, byte[]> jarClasses,
                             String name, java.util.function.Consumer<ClassNode> action) {
        action.accept(cn);
        tryWrite(cr, cn, jarClasses, name);
    }

    private static void tryWrite(ClassReader cr, ClassNode cn, Map<String, byte[]> jarClasses, String step) {
        try {
            byte[] out = JarClassWriter.writeComputed(cr, cn, jarClasses);
            JvmVerify.defineAndVerify(cn.name, out);
            System.out.println("OK " + step + " (" + out.length + " bytes)");
        } catch (Throwable t) {
            System.out.println("FAIL " + step + ": " + t.getClass().getSimpleName() + " " + t.getMessage());
            Throwable c = t.getCause();
            if (c != null) {
                System.out.println("  cause: " + c.getClass().getSimpleName() + " " + c.getMessage());
            }
        }
    }

    private static ClassNode clone(ClassNode src) {
        ClassReader cr = new ClassReader(writeNode(src));
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);
        return cn;
    }

    private static byte[] writeNode(ClassNode cn) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static void stripMutable(MethodNode m) {
        List<org.objectweb.asm.tree.AbstractInsnNode> trash = new ArrayList<>();
        for (var n = m.instructions.getFirst(); n != null; n = n.getNext()) {
            if (n instanceof org.objectweb.asm.tree.MethodInsnNode min
                    && min.owner.startsWith("org/apache/commons/lang3/MutableUtilities")) {
                trash.add(n);
            }
        }
        trash.forEach(m.instructions::remove);
    }
}
