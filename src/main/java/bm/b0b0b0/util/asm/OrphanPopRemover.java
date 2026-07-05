package bm.b0b0b0.util.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

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
        for (int scan = 0; scan < 64 && removeGotoPopReturnPatterns(method); scan++) {
            changed = true;
        }
        for (int attempt = 0; attempt < 64; attempt++) {
            try {
                new Analyzer<>(new BasicInterpreter()).analyze(owner, method);
                return changed;
            } catch (AnalyzerException e) {
                AbstractInsnNode bad = firstRealInsn(e.node);
                if (bad == null) {
                    return changed;
                }
                int op = bad.getOpcode();
                if (op == Opcodes.POP || op == Opcodes.POP2) {
                    method.instructions.remove(bad);
                    changed = true;
                    continue;
                }
                if (bad instanceof JumpInsnNode jump && jump.getOpcode() == Opcodes.GOTO) {
                    if (foldGotoPopReturn(method, jump)) {
                        changed = true;
                        continue;
                    }
                }
                return changed;
            }
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
