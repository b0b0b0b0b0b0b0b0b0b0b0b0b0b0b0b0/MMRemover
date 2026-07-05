package bm.b0b0b0.util.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class MutableUtilitiesStub {

    private static final String INTERNAL = "org/apache/commons/lang3/MutableUtilities";

    private MutableUtilitiesStub() {
    }

    public static byte[] classBytes() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                INTERNAL,
                null,
                "java/lang/Object",
                new String[]{"org/bukkit/event/Listener"});

        MethodVisitor init = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "(Lorg/bukkit/plugin/Plugin;)V",
                null,
                null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 2);
        init.visitEnd();

        MethodVisitor run = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "run",
                "()V",
                null,
                null);
        run.visitCode();
        run.visitInsn(Opcodes.RETURN);
        run.visitMaxs(0, 1);
        run.visitEnd();

        MethodVisitor start = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "start",
                "()V",
                null,
                null);
        start.visitCode();
        start.visitInsn(Opcodes.RETURN);
        start.visitMaxs(0, 1);
        start.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
