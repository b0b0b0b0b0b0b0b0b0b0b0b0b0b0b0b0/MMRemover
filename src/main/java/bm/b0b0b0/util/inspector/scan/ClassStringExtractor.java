package bm.b0b0b0.util.inspector.scan;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ClassStringExtractor {

    private ClassStringExtractor() {
    }

    public static Set<String> extract(byte[] classBytes) {
        Set<String> strings = new LinkedHashSet<>();
        try {
            ClassReader reader = new ClassReader(classBytes);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature,
                                  String superName, String[] interfaces) {
                    add(strings, name);
                    add(strings, superName);
                    if (interfaces != null) {
                        for (String iface : interfaces) {
                            add(strings, iface);
                        }
                    }
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitLdcInsn(Object value) {
                            if (value instanceof String string) {
                                strings.add(string);
                            }
                        }

                        @Override
                        public void visitInvokeDynamicInsn(String name, String descriptor,
                                                           Handle bootstrapMethodHandle,
                                                           Object... bootstrapMethodArguments) {
                            for (Object arg : bootstrapMethodArguments) {
                                if (arg instanceof String string) {
                                    strings.add(string);
                                }
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG);
        } catch (Throwable ignored) {
        }
        return strings;
    }

    private static void add(Set<String> sink, String value) {
        if (value != null && !value.isEmpty()) {
            sink.add(value);
        }
    }
}
