package bm.b0b0b0.util.dev;

import bm.b0b0b0.hacks.chbkHack;
import bm.b0b0b0.util.asm.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class StepVoice {
    public static void main(String[] args) throws Exception {
        byte[] raw;
        try (ZipFile z = new ZipFile(Path.of("E:/IntelliJ/MMRemover/input/voicechat-bukkit-2.6.20.jar").toFile())) {
            raw = z.getInputStream(new ZipEntry("de/maxhenkel/voicechat/Voicechat.class")).readAllBytes();
        }
        ClassReader cr = new ClassReader(raw);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG);
        MethodNode m = cn.methods.stream().filter(x -> "onEnable".equals(x.name)).findFirst().orElseThrow();
        show(m, "0 orig tree");
        Method stripCalls = chbkHack.class.getDeclaredMethod("stripCalls", MethodNode.class);
        stripCalls.setAccessible(true);
        stripCalls.invoke(null, m);
        show(m, "1 stripCalls");
        DeadTryCatchRemover.cleanMethod(m);
        show(m, "2 deadTry");
        Method stripChambor = chbkHack.class.getDeclaredMethod("stripChamborCatchAll", MethodNode.class, boolean.class);
        stripChambor.setAccessible(true);
        stripChambor.invoke(null, m, true);
        show(m, "3 chambor");
        OrphanPopRemover.cleanMethod(cn.name, m);
        show(m, "4 orphan");
        BytecodeSanitizer.sanitizeChangedMethod(cn.name, m);
        show(m, "5 sanitize");
        java.util.Map<String, byte[]> jarClasses = new java.util.HashMap<>();
        try (ZipFile z = new ZipFile(Path.of("E:/IntelliJ/MMRemover/input/voicechat-bukkit-2.6.20.jar").toFile())) {
            var it = z.entries();
            while (it.hasMoreElements()) {
                var e = it.nextElement();
                if (!e.isDirectory() && e.getName().endsWith(".class")) {
                    byte[] d = z.getInputStream(e).readAllBytes();
                    jarClasses.put(e.getName(), d);
                    jarClasses.put(e.getName().substring(0, e.getName().length() - 6), d);
                }
            }
        }
        try {
            FrameRecomputer.recomputeOne(cn, m, jarClasses);
            show(m, "6 frames");
        } catch (Throwable t) {
            System.out.println("6 frames FAIL " + t.getClass().getSimpleName() + " " + t.getMessage());
        }
        try {
            java.util.Set<String> touched = java.util.Set.of("onEnable" + m.desc);
            byte[] out = JarClassWriter.writePatchedMethods(cr, cn, jarClasses, touched);
            showWritten(out, "7 writePatched");
        } catch (Throwable t) {
            System.out.println("7 writePatched FAIL " + t.getClass().getSimpleName() + " " + t.getMessage());
            byte[] out = JarClassWriter.emitComputed(cr, cn, jarClasses);
            showWritten(out, "7 emitComputed");
        }
    }

    private static void showWritten(byte[] raw, String step) throws Exception {
        ClassReader cr2 = new ClassReader(raw);
        ClassNode cn2 = new ClassNode();
        cr2.accept(cn2, 0);
        MethodNode m2 = cn2.methods.stream().filter(x -> "onEnable".equals(x.name)).findFirst().orElseThrow();
        show(m2, step);
    }

    private static void show(MethodNode m, String step) {
        for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
            if (n instanceof org.objectweb.asm.tree.LdcInsnNode ldc
                    && ldc.cst != null && ldc.cst.toString().contains("PlaceholderAPI")) {
                for (AbstractInsnNode x = n; x != null; x = x.getNext()) {
                    if (x instanceof MethodInsnNode min && "getPlugin".equals(min.name)) {
                        AbstractInsnNode after = x.getNext();
                        while (after != null && after.getType() == AbstractInsnNode.LABEL) {
                            after = after.getNext();
                        }
                        System.out.println(step + " after getPlugin op=" + after.getOpcode()
                                + (after.getOpcode() == Opcodes.IFNULL ? " IFNULL" : ""));
                        return;
                    }
                }
            }
        }
    }
}
