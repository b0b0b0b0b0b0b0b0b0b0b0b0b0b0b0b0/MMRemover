package bm.b0b0b0.util.asm;

import java.util.Map;

final class JarBytecodeClassLoader extends ClassLoader {

    private final Map<String, byte[]> classes;

    JarBytecodeClassLoader(Map<String, byte[]> classes, ClassLoader parent) {
        super(parent);
        this.classes = classes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String internal = name.replace('.', '/');
        byte[] bytes = classes.get(internal);
        if (bytes == null) {
            bytes = classes.get(internal + ".class");
        }
        if (bytes != null) {
            return defineClass(name, bytes, 0, bytes.length);
        }
        return super.findClass(name);
    }
}
