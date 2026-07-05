package bm.b0b0b0.util.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class DeadTryCatchRemover {

    private static final int HANDLER_SCAN_LIMIT = 256;

    private DeadTryCatchRemover() {
    }

    public static void clean(ClassNode cn) {
        for (MethodNode method : cn.methods) {
            cleanMethod(method);
        }
    }

    public static void cleanMethod(MethodNode method) {
        if (method.tryCatchBlocks != null && !method.tryCatchBlocks.isEmpty()) {
            Iterator<TryCatchBlockNode> it = method.tryCatchBlocks.iterator();
            while (it.hasNext()) {
                TryCatchBlockNode block = it.next();
                if (isMalwareCatch(method, block)) {
                    dropHandlerBlock(method, block);
                    it.remove();
                }
            }
        }
        repairHandlerJumps(method);
    }

    public static void repairHandlerJumps(MethodNode method) {
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (!(insn instanceof JumpInsnNode jump) || jump.getOpcode() != Opcodes.GOTO) {
                continue;
            }
            if (isOrphanHandlerLabel(jump.label)) {
                replaceJumpWithReturn(method, jump);
            }
        }
    }

    private static boolean isOrphanHandlerLabel(LabelNode label) {
        for (AbstractInsnNode insn = label; insn != null; insn = insn.getNext()) {
            if (insn instanceof LabelNode || insn instanceof LineNumberNode || insn instanceof FrameNode) {
                continue;
            }
            return isReturnOpcode(insn.getOpcode());
        }
        return true;
    }

    private static void replaceJumpWithReturn(MethodNode method, JumpInsnNode jump) {
        Type returnType = Type.getReturnType(method.desc);
        if (returnType == Type.VOID_TYPE) {
            method.instructions.set(jump, new InsnNode(Opcodes.RETURN));
            return;
        }
        InsnList repl = new InsnList();
        switch (returnType.getSort()) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> {
                repl.add(new InsnNode(Opcodes.ICONST_0));
                repl.add(new InsnNode(Opcodes.IRETURN));
            }
            case Type.LONG -> {
                repl.add(new InsnNode(Opcodes.LCONST_0));
                repl.add(new InsnNode(Opcodes.LRETURN));
            }
            case Type.FLOAT -> {
                repl.add(new InsnNode(Opcodes.FCONST_0));
                repl.add(new InsnNode(Opcodes.FRETURN));
            }
            case Type.DOUBLE -> {
                repl.add(new InsnNode(Opcodes.DCONST_0));
                repl.add(new InsnNode(Opcodes.DRETURN));
            }
            default -> {
                repl.add(new InsnNode(Opcodes.ACONST_NULL));
                repl.add(new InsnNode(Opcodes.ARETURN));
            }
        }
        method.instructions.insert(jump, repl);
        method.instructions.remove(jump);
    }

    public static boolean isMalwareCatch(MethodNode method, TryCatchBlockNode block) {
        if (block.handler == null) {
            return true;
        }
        if (block.type == null) {
            if (isSyncCatch(method, block)) {
                return false;
            }
            return shouldDrop(method, block);
        }
        if (shouldDrop(method, block)) {
            return true;
        }
        return false;
    }

    public static void dropHandlerBlock(MethodNode method, TryCatchBlockNode block) {
        if (block == null || block.handler == null) {
            return;
        }
        neutralizeHandlerGotos(method, block.handler);
        removeHandlerInstructions(method, block.handler);
    }

    private static boolean isSyncCatch(MethodNode method, TryCatchBlockNode block) {
        if (block.start == null || block.end == null) {
            return false;
        }
        boolean inRange = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn == block.start) {
                inRange = true;
            }
            if (!inRange) {
                continue;
            }
            if (insn.getOpcode() == Opcodes.MONITORENTER) {
                return true;
            }
            if (insn == block.end) {
                return false;
            }
        }
        return false;
    }

    private static boolean shouldDrop(MethodNode method, TryCatchBlockNode block) {
        if (block.handler == null) {
            return true;
        }
        return isRethrowStub(block.handler) || isSwallowStub(block.handler);
    }

    private static boolean isRethrowStub(LabelNode handler) {
        List<AbstractInsnNode> real = collectHandlerInsns(handler);
        if (real.isEmpty()) {
            return true;
        }
        if (real.size() == 1 && real.get(0).getOpcode() == Opcodes.ATHROW) {
            return true;
        }
        if (real.size() == 1 && real.get(0).getOpcode() == Opcodes.GOTO) {
            return true;
        }
        if (real.size() == 1 && isReturnOpcode(real.get(0).getOpcode())) {
            return true;
        }
        if (real.size() == 2
                && real.get(0).getOpcode() == Opcodes.ALOAD
                && real.get(1).getOpcode() == Opcodes.ATHROW) {
            return true;
        }
        if (real.size() == 3
                && real.get(0) instanceof VarInsnNode store
                && store.getOpcode() == Opcodes.ASTORE
                && real.get(1) instanceof VarInsnNode load
                && load.getOpcode() == Opcodes.ALOAD
                && load.var == store.var
                && real.get(2).getOpcode() == Opcodes.ATHROW) {
            return true;
        }
        return isLogAndGotoStub(real);
    }

    private static boolean isSwallowStub(LabelNode handler) {
        List<AbstractInsnNode> real = collectHandlerInsns(handler);
        if (real.isEmpty()) {
            return true;
        }
        if (real.size() == 1 && isReturnOpcode(real.get(0).getOpcode())) {
            return true;
        }
        if (real.size() == 2) {
            int first = real.get(0).getOpcode();
            int second = real.get(1).getOpcode();
            if ((first == Opcodes.POP || first == Opcodes.POP2) && isReturnOpcode(second)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReturnOpcode(int op) {
        return op == Opcodes.RETURN
                || op == Opcodes.IRETURN
                || op == Opcodes.LRETURN
                || op == Opcodes.FRETURN
                || op == Opcodes.DRETURN
                || op == Opcodes.ARETURN;
    }

    private static void neutralizeHandlerGotos(MethodNode method, LabelNode handler) {
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (!(insn instanceof JumpInsnNode jump) || jump.label != handler) {
                continue;
            }
            replaceJumpWithReturn(method, jump);
        }
    }

    private static boolean isLogAndGotoStub(List<AbstractInsnNode> real) {
        if (real.size() < 2 || real.size() > 6) {
            return false;
        }
        boolean hasAthrow = false;
        boolean hasGoto = false;
        for (AbstractInsnNode insn : real) {
            int op = insn.getOpcode();
            if (op == Opcodes.ATHROW) {
                hasAthrow = true;
            }
            if (op == Opcodes.GOTO) {
                hasGoto = true;
            }
            if (insn instanceof MethodInsnNode min
                    && ("printStackTrace".equals(min.name) || "getStackTrace".equals(min.name))) {
                return false;
            }
        }
        if (!hasGoto || hasAthrow) {
            return false;
        }
        return real.get(0) instanceof VarInsnNode store && store.getOpcode() == Opcodes.ASTORE;
    }

    private static List<AbstractInsnNode> collectHandlerInsns(LabelNode handler) {
        List<AbstractInsnNode> out = new ArrayList<>();
        int count = 0;
        for (AbstractInsnNode insn = handler; insn != null && count < HANDLER_SCAN_LIMIT; insn = insn.getNext()) {
            if (insn instanceof LabelNode || insn instanceof LineNumberNode || insn instanceof FrameNode) {
                continue;
            }
            out.add(insn);
            count++;
            if (isHandlerEnd(insn.getOpcode())) {
                break;
            }
        }
        return out;
    }

    private static boolean isHandlerEnd(int op) {
        if (op < 0) {
            return false;
        }
        return switch (op) {
            case Opcodes.GOTO, Opcodes.ATHROW,
                 Opcodes.RETURN, Opcodes.IRETURN, Opcodes.LRETURN,
                 Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN -> true;
            default -> false;
        };
    }

    private static void removeHandlerInstructions(MethodNode method, LabelNode handler) {
        List<AbstractInsnNode> toRemove = new ArrayList<>();
        for (AbstractInsnNode insn = handler; insn != null; ) {
            AbstractInsnNode next = insn.getNext();
            if (insn instanceof LabelNode) {
                insn = next;
                continue;
            }
            if (insn instanceof LineNumberNode || insn instanceof FrameNode) {
                toRemove.add(insn);
                insn = next;
                continue;
            }
            int op = insn.getOpcode();
            toRemove.add(insn);
            if (isHandlerEnd(op)) {
                break;
            }
            insn = next;
        }
        toRemove.forEach(method.instructions::remove);
    }
}
