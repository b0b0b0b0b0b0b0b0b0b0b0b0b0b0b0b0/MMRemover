package bm.b0b0b0.util.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class TryCatchBlockPruner {

    private TryCatchBlockPruner() {
    }

    public static void prune(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            prune(method);
        }
    }

    public static void prune(MethodNode method) {
        List<TryCatchBlockNode> blocks = method.tryCatchBlocks;
        if (blocks == null || blocks.isEmpty()) {
            return;
        }

        Set<LabelNode> live = liveLabels(method);
        Iterator<TryCatchBlockNode> it = blocks.iterator();
        while (it.hasNext()) {
            TryCatchBlockNode block = it.next();
            if (block.start == null || block.end == null || block.handler == null) {
                it.remove();
                continue;
            }
            if (!live.contains(block.start) || !live.contains(block.end) || !live.contains(block.handler)) {
                it.remove();
                continue;
            }
            if (!isValidRange(method, block.start, block.end)) {
                it.remove();
                continue;
            }
            if (!rangeHasRealInsn(method, block.start, block.end)) {
                it.remove();
                continue;
            }
            if (!rangeEmitsBytecode(method, block.start, block.end)) {
                it.remove();
                continue;
            }
            if (isGotoOnlyTry(method, block)) {
                removeGotoOnlyTry(method, block);
                it.remove();
            }
        }
    }

    static boolean isValidTryRange(MethodNode method, LabelNode start, LabelNode end) {
        return isValidRange(method, start, end);
    }

    private static Set<LabelNode> liveLabels(MethodNode method) {
        Set<LabelNode> live = new HashSet<>();
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LabelNode label) {
                live.add(label);
            }
        }
        return live;
    }

    private static boolean isValidRange(MethodNode method, LabelNode start, LabelNode end) {
        if (start == end) {
            return false;
        }
        int startIdx = method.instructions.indexOf(start);
        int endIdx = method.instructions.indexOf(end);
        if (startIdx < 0 || endIdx < 0 || startIdx >= endIdx) {
            return false;
        }
        return true;
    }

    private static boolean rangeHasRealInsn(MethodNode method, LabelNode start, LabelNode end) {
        boolean inRange = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn == start) {
                inRange = true;
            }
            if (!inRange) {
                continue;
            }
            if (insn == end) {
                return false;
            }
            if (isRealInsn(insn)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRealInsn(AbstractInsnNode insn) {
        int type = insn.getType();
        return type != AbstractInsnNode.LABEL
                && type != AbstractInsnNode.LINE
                && type != AbstractInsnNode.FRAME;
    }

    private static boolean rangeEmitsBytecode(MethodNode method, LabelNode start, LabelNode end) {
        boolean inRange = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn == start) {
                inRange = true;
            }
            if (!inRange) {
                continue;
            }
            if (insn == end) {
                return false;
            }
            if (insn.getOpcode() >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGotoOnlyTry(MethodNode method, TryCatchBlockNode block) {
        boolean inRange = false;
        JumpInsnNode onlyGoto = null;
        int bytecodeInsns = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn == block.start) {
                inRange = true;
            }
            if (!inRange) {
                continue;
            }
            if (insn == block.end) {
                break;
            }
            if (insn.getOpcode() < 0) {
                continue;
            }
            bytecodeInsns++;
            if (insn instanceof JumpInsnNode jump && jump.getOpcode() == Opcodes.GOTO) {
                onlyGoto = jump;
                continue;
            }
            return false;
        }
        return bytecodeInsns == 1 && onlyGoto != null;
    }

    private static void removeGotoOnlyTry(MethodNode method, TryCatchBlockNode block) {
        boolean inRange = false;
        for (AbstractInsnNode insn : method.instructions) {
            if (insn == block.start) {
                inRange = true;
            }
            if (!inRange) {
                continue;
            }
            if (insn == block.end) {
                break;
            }
            if (insn instanceof JumpInsnNode jump && jump.getOpcode() == Opcodes.GOTO) {
                method.instructions.remove(jump);
            }
        }
        DeadTryCatchRemover.dropHandlerBlock(method, block);
        removeSelfLoopGotos(method);
        DeadTryCatchRemover.repairHandlerJumps(method);
    }

    private static void removeSelfLoopGotos(MethodNode method) {
        for (AbstractInsnNode insn : method.instructions) {
            if (!(insn instanceof JumpInsnNode jump) || jump.getOpcode() != Opcodes.GOTO) {
                continue;
            }
            LabelNode target = jump.label;
            AbstractInsnNode cursor = jump.getPrevious();
            while (cursor != null
                    && (cursor instanceof LabelNode || cursor.getType() == AbstractInsnNode.LINE
                    || cursor instanceof org.objectweb.asm.tree.FrameNode)) {
                if (cursor == target) {
                    method.instructions.remove(jump);
                    break;
                }
                cursor = cursor.getPrevious();
            }
        }
    }
}
