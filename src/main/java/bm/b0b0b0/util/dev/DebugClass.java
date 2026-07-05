package bm.b0b0b0.util.dev;

import bm.b0b0b0.hacks.chbkHack;
import bm.b0b0b0.util.files.JarEntryUtils;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class DebugClass {
    public static void main(String[] args) throws Exception {
        String jarName = args[0];
        String className = args[1];
        File jar = new File("input", jarName);
        Map<String, byte[]> jarClasses = new HashMap<>();
        JarEntryUtils.Entry target = null;
        for (JarEntryUtils.Entry e : JarEntryUtils.readDeduped(jar)) {
            if (!e.isDirectory() && e.name().endsWith(".class") && e.data() != null) {
                jarClasses.put(e.name(), e.data());
                jarClasses.put(e.name().substring(0, e.name().length() - 6), e.data());
                if (e.name().equals(className + ".class")) {
                    target = e;
                }
            }
        }
        if (target == null) {
            System.out.println("missing");
            return;
        }
        Constructor<chbkHack> ctor = chbkHack.class.getDeclaredConstructor(Conf.class, Path.class);
        ctor.setAccessible(true);
        chbkHack hack = ctor.newInstance(new Conf(), Path.of("."));
        Method patch = chbkHack.class.getDeclaredMethod("patchMutableUtilitiesCalls", byte[].class, Map.class);
        patch.setAccessible(true);
        try {
            byte[] out = (byte[]) patch.invoke(hack, target.data(), jarClasses);
            System.out.println("patched " + out.length);
            dumpTryCatch(out);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable t = ite.getCause();
            System.out.println(t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace(System.out);
        }
    }

    private static void dumpTryCatch(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);
        for (MethodNode m : cn.methods) {
            if (m.tryCatchBlocks == null || m.tryCatchBlocks.isEmpty()) {
                continue;
            }
            System.out.println(m.name + m.desc + " blocks=" + m.tryCatchBlocks.size());
            int i = 0;
            for (TryCatchBlockNode b : m.tryCatchBlocks) {
                int s = m.instructions.indexOf(b.start);
                int e = m.instructions.indexOf(b.end);
                int h = m.instructions.indexOf(b.handler);
                System.out.println("  #" + (i++) + " start=" + s + " end=" + e + " handler=" + h + " empty=" + (s >= e));
            }
        }
    }
}
