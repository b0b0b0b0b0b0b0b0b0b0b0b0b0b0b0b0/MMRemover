package bm.b0b0b0.util.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

public final class MethodBodyGuard {

    private MethodBodyGuard() {
    }

    public static void ensureClassBodies(ClassNode cn) {
        for (MethodNode method : cn.methods) {
            ensureMethodBody(method);
        }
    }

    public static void ensureMethodBody(MethodNode method) {
        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
            return;
        }
        if (hasExecutableCode(method)) {
            return;
        }
        if (method.instructions == null) {
            method.instructions = new InsnList();
        } else {
            method.instructions.clear();
        }
        if (method.tryCatchBlocks != null) {
            method.tryCatchBlocks.clear();
        }
        if (method.exceptions != null) {
            method.exceptions.clear();
        }
        method.instructions.add(new InsnNode(returnOpcode(method)));
    }

    private static boolean hasExecutableCode(MethodNode method) {
        if (method.instructions == null || method.instructions.size() == 0) {
            return false;
        }
        for (var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LabelNode || insn instanceof LineNumberNode || insn instanceof FrameNode) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static int returnOpcode(MethodNode method) {
        return switch (Type.getReturnType(method.desc).getSort()) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> Opcodes.IRETURN;
            case Type.FLOAT -> Opcodes.FRETURN;
            case Type.LONG -> Opcodes.LRETURN;
            case Type.DOUBLE -> Opcodes.DRETURN;
            case Type.ARRAY, Type.OBJECT -> Opcodes.ARETURN;
            default -> Opcodes.RETURN;
        };
    }
}
