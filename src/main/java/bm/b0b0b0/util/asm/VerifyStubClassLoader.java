package bm.b0b0b0.util.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VerifyStubClassLoader extends ClassLoader {

    private final Map<String, byte[]> jarClasses;
    private final Map<String, Class<?>> defined = new ConcurrentHashMap<>();

    public VerifyStubClassLoader(Map<String, byte[]> jarClasses, ClassLoader parent) {
        super(parent);
        this.jarClasses = jarClasses != null ? jarClasses : Map.of();
    }

    public void predefineJarClasses() {
        Map<String, byte[]> pending = new HashMap<>();
        for (Map.Entry<String, byte[]> e : jarClasses.entrySet()) {
            String key = e.getKey();
            if (key.endsWith(".class") || e.getValue() == null) {
                continue;
            }
            pending.put(key, e.getValue());
        }
        int last;
        do {
            last = pending.size();
            pending.entrySet().removeIf(entry -> {
                String internal = entry.getKey();
                String binary = internal.replace('/', '.');
                if (defined.containsKey(binary)) {
                    return true;
                }
                try {
                    define(binary, entry.getValue());
                    return true;
                } catch (LinkageError ignored) {
                    return false;
                }
            });
        } while (!pending.isEmpty() && pending.size() < last);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> cached = defined.get(name);
        if (cached != null) {
            return cached;
        }
        String internal = name.replace('.', '/');
        byte[] bytes = jarClasses.get(internal);
        if (bytes != null) {
            return define(name, bytes);
        }
        return defineStub(name);
    }

    public Class<?> define(String binaryName, byte[] bytes) {
        Class<?> existing = defined.get(binaryName);
        if (existing != null) {
            return existing;
        }
        Class<?> clazz = defineClass(binaryName, bytes, 0, bytes.length);
        defined.put(binaryName, clazz);
        return clazz;
    }

    private Class<?> defineStub(String binaryName) {
        Class<?> existing = defined.get(binaryName);
        if (existing != null) {
            return existing;
        }
        String internal = binaryName.replace('.', '/');
        StubShape shape = STUBS.getOrDefault(internal, StubShape.object());
        byte[] bytes = shape.emit(internal);
        Class<?> clazz = defineClass(binaryName, bytes, 0, bytes.length);
        defined.put(binaryName, clazz);
        return clazz;
    }

    public Class<?> verifyClass(String binaryName) throws ClassNotFoundException {
        return loadClass(binaryName, true);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")) {
                    c = getParent().loadClass(name);
                } else {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException e) {
                        c = getParent().loadClass(name);
                    }
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    public String commonSuperClass(String type1, String type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        if ("java/lang/Object".equals(type1) || "java/lang/Object".equals(type2)) {
            return "java/lang/Object";
        }
        try {
            Class<?> c1 = loadClass(type1.replace('/', '.'));
            Class<?> c2 = loadClass(type2.replace('/', '.'));
            if (c1.isAssignableFrom(c2)) {
                return type1;
            }
            if (c2.isAssignableFrom(c1)) {
                return type2;
            }
            if (!c1.isInterface() && !c2.isInterface()) {
                Class<?> walk = c1;
                while (walk != null && !walk.isAssignableFrom(c2)) {
                    walk = walk.getSuperclass();
                }
                if (walk != null) {
                    return walk.getName().replace('.', '/');
                }
            }
        } catch (Throwable ignored) {
        }
        return "java/lang/Object";
    }

    private record StubShape(int access, String superName, String[] interfaces) {
        static StubShape object() {
            return new StubShape(Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "java/lang/Object", null);
        }

        static StubShape iface() {
            return new StubShape(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE, "java/lang/Object", null);
        }

        byte[] emit(String internal) {
            ClassWriter cw = new ClassWriter(0);
            cw.visit(Opcodes.V17, access, internal, null, superName, interfaces);
            cw.visitEnd();
            return cw.toByteArray();
        }
    }

    private static final Map<String, StubShape> STUBS = Map.ofEntries(
            Map.entry("org/bukkit/plugin/Plugin", StubShape.iface()),
            Map.entry("org/bukkit/plugin/java/JavaPlugin", new StubShape(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "java/lang/Object", new String[]{"org/bukkit/plugin/Plugin"})),
            Map.entry("org/bukkit/Server", StubShape.iface()),
            Map.entry("org/bukkit/plugin/PluginManager", StubShape.iface()),
            Map.entry("org/bukkit/command/CommandSender", StubShape.iface()),
            Map.entry("org/bukkit/command/Command", StubShape.object()),
            Map.entry("org/bukkit/command/PluginCommand", StubShape.object()),
            Map.entry("org/bukkit/entity/Player", StubShape.iface()),
            Map.entry("org/bukkit/configuration/file/FileConfiguration", StubShape.object()),
            Map.entry("org/bukkit/configuration/Configuration", StubShape.iface())
    );
}
