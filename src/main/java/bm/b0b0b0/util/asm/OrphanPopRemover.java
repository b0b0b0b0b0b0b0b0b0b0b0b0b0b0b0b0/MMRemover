package bm.b0b0b0.util.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

public final class OrphanPopRemover {

    private OrphanPopRemover() {
    }

    public static void clean(ClassNode cn) {
        boolean changed;
        do {
            changed = false;
            for (MethodNode method : cn.methods) {
                if (cleanMethod(cn.name, method)) {
                    changed = true;
                }
            }
        } while (changed);
    }

    public static boolean cleanMethod(String owner, MethodNode method) {
        boolean changed = false;
        for (int scan = 0; scan < 16 && removeGotoPopReturnPatterns(method); scan++) {
            changed = true;
        }
        return changed;
    }

    private static boolean removeGotoPopReturnPatterns(MethodNode method) {
        boolean changed = false;
        for (AbstractInsnNode n : method.instructions.toArray()) {
            if (n instanceof JumpInsnNode jump && jump.getOpcode() == Opcodes.GOTO) {
                if (foldGotoPopReturn(method, jump)) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean foldGotoPopReturn(MethodNode method, JumpInsnNode jump) {
        AbstractInsnNode target = firstRealInsn(jump.label);
        if (target == null) {
            return false;
        }
        if (hasRealCodeBetween(jump, jump.label)) {
            return false;
        }
        if (target.getOpcode() == Opcodes.POP || target.getOpcode() == Opcodes.POP2) {
            AbstractInsnNode afterPop = firstRealInsn(target.getNext());
            if (afterPop != null && afterPop.getOpcode() == Opcodes.RETURN) {
                method.instructions.remove(target);
                method.instructions.remove(jump);
                return true;
            }
        }
        return false;
    }

    private static boolean hasRealCodeBetween(JumpInsnNode jump, LabelNode label) {
        for (AbstractInsnNode n = jump.getNext(); n != null && n != label; n = n.getNext()) {
            if (n instanceof LabelNode || n instanceof LineNumberNode || n instanceof FrameNode) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static AbstractInsnNode firstRealInsn(AbstractInsnNode start) {
        for (AbstractInsnNode n = start; n != null; n = n.getNext()) {
            if (n instanceof LabelNode || n instanceof LineNumberNode || n instanceof FrameNode) {
                continue;
            }
            return n;
        }
        return null;
    }
}
