package bm.b0b0b0.util.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class CatchHandlerLocalFixer {

    private CatchHandlerLocalFixer() {
    }

    public static void fix(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            fix(method);
        }
    }

    public static boolean needsFix(MethodNode method, int maxInsns) {
        if (method.tryCatchBlocks == null || method.tryCatchBlocks.isEmpty()) {
            return false;
        }
        if (method.instructions.size() > maxInsns) {
            return false;
        }
        for (TryCatchBlockNode block : method.tryCatchBlocks) {
            if (block.handler == null || isPopSwallowHandler(method, block.handler)) {
                continue;
            }
            if (!isConfigIoCatch(block.type)) {
                continue;
            }
            if (handlerLogsThrowable(method, block.handler)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isConfigIoCatch(String type) {
        return "java/io/IOException".equals(type)
                || "org/bukkit/configuration/InvalidConfigurationException".equals(type);
    }

    public static void fix(MethodNode method) {
        List<TryCatchBlockNode> blocks = method.tryCatchBlocks;
        if (blocks == null || blocks.isEmpty()) {
            return;
        }

        Map<LabelNode, LabelNode> entryForHandler = new HashMap<>();
        for (TryCatchBlockNode block : blocks) {
            if (block.handler == null || isPopSwallowHandler(method, block.handler)) {
                continue;
            }
            entryForHandler.computeIfAbsent(block.handler, h -> {
                LabelNode entry = new LabelNode();
                method.instructions.insertBefore(h, entry);
                return entry;
            });
            block.handler = entryForHandler.get(block.handler);
        }

        Set<Integer> dedicated = new HashSet<>();
        Map<LabelNode, Integer> entrySlots = new HashMap<>();
        for (Map.Entry<LabelNode, LabelNode> mapping : entryForHandler.entrySet()) {
            LabelNode merge = mapping.getKey();
            LabelNode entry = mapping.getValue();

            VarInsnNode storeAtMerge = findCatchStore(method, merge);
            int newSlot = method.maxLocals++;
            while (dedicated.contains(newSlot)) {
                newSlot = method.maxLocals++;
            }
            dedicated.add(newSlot);
            entrySlots.put(entry, newSlot);

            InsnList prologue = new InsnList();
            prologue.add(new VarInsnNode(Opcodes.ASTORE, newSlot));
            if (storeAtMerge != null) {
                int oldSlot = storeAtMerge.var;
                AbstractInsnNode afterStore = nextReal(storeAtMerge.getNext());
                method.instructions.remove(storeAtMerge);
                if (afterStore != null && afterStore != merge) {
                    prologue.add(new JumpInsnNode(Opcodes.GOTO, labelBefore(method, afterStore)));
                }
                remapHandlerVar(method, merge, oldSlot, newSlot);
            }
            method.instructions.insert(entry, prologue);

            castThrowableArgsInHandler(method, merge);
            castThrowableArgsInHandler(method, entry);
        }
    }

    private static LabelNode labelBefore(MethodNode method, AbstractInsnNode insn) {
        if (insn instanceof LabelNode label) {
            return label;
        }
        LabelNode bridge = new LabelNode();
        method.instructions.insertBefore(insn, bridge);
        return bridge;
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode insn) {
        while (insn != null && (insn.getType() == AbstractInsnNode.LABEL
                || insn.getType() == AbstractInsnNode.LINE
                || insn.getType() == AbstractInsnNode.FRAME)) {
            insn = insn.getNext();
        }
        return insn;
    }

    private static boolean handlerLogsThrowable(MethodNode method, LabelNode handler) {
        final boolean[] found = {false};
        forEachHandlerInsn(method, handler, insn -> {
            if (!(insn instanceof MethodInsnNode min)) {
                return;
            }
            if ("java/util/logging/Logger".equals(min.owner)
                    && "(Ljava/util/logging/Level;Ljava/lang/String;Ljava/lang/Throwable;)V".equals(min.desc)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static boolean isPopSwallowHandler(MethodNode method, LabelNode handler) {
        AbstractInsnNode first = firstHandlerInsn(method, handler);
        if (first == null) {
            return false;
        }
        int op = first.getOpcode();
        return op == Opcodes.POP || op == Opcodes.POP2;
    }

    private static AbstractInsnNode firstHandlerInsn(MethodNode method, LabelNode handler) {
        final AbstractInsnNode[] found = new AbstractInsnNode[1];
        forEachHandlerInsn(method, handler, insn -> {
            if (found[0] == null) {
                found[0] = insn;
            }
        });
        return found[0];
    }

    private static VarInsnNode findCatchStore(MethodNode method, LabelNode handler) {
        if (isPopSwallowHandler(method, handler)) {
            return null;
        }
        final VarInsnNode[] found = new VarInsnNode[1];
        forEachHandlerInsn(method, handler, insn -> {
            if (found[0] == null && insn instanceof VarInsnNode var && var.getOpcode() == Opcodes.ASTORE) {
                found[0] = var;
            }
        });
        return found[0];
    }

    private static void remapHandlerVar(MethodNode method, LabelNode handler, int from, int to) {
        if (from < 0 || from == to) {
            return;
        }
        forEachHandlerInsn(method, handler, insn -> {
            if (insn instanceof VarInsnNode var && var.var == from) {
                var.var = to;
            }
        });
    }

    private static void castThrowableArgsInHandler(MethodNode method, LabelNode handler) {
        forEachHandlerInsn(method, handler, insn -> {
            if (!(insn instanceof MethodInsnNode min)) {
                return;
            }
            if (!methodUsesThrowableParam(min.desc)) {
                return;
            }
            AbstractInsnNode prev = previousReal(insn);
            if (prev instanceof TypeInsnNode type && type.getOpcode() == Opcodes.CHECKCAST) {
                return;
            }
            if (prev instanceof VarInsnNode var && var.getOpcode() == Opcodes.ALOAD) {
                InsnList cast = new InsnList();
                cast.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Throwable"));
                method.instructions.insertBefore(insn, cast);
            }
        });
    }

    private static void forEachHandlerInsn(MethodNode method, LabelNode handler, Consumer<AbstractInsnNode> action) {
        boolean in = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn == handler) {
                in = true;
                continue;
            }
            if (!in) {
                continue;
            }
            if (insn instanceof LabelNode || insn.getType() == AbstractInsnNode.LINE
                    || insn.getType() == AbstractInsnNode.FRAME) {
                continue;
            }
            action.accept(insn);
            if (HandlerInsn.isTerminator(insn.getOpcode())) {
                break;
            }
        }
    }

    private static boolean methodUsesThrowableParam(String desc) {
        for (Type arg : Type.getArgumentTypes(desc)) {
            if (arg.getSort() != Type.OBJECT) {
                continue;
            }
            String internal = arg.getInternalName();
            if ("java/lang/Throwable".equals(internal)
                    || "java/lang/Exception".equals(internal)
                    || "java/lang/Error".equals(internal)) {
                return true;
            }
        }
        return false;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode insn) {
        AbstractInsnNode p = insn.getPrevious();
        while (p != null && (p.getType() == AbstractInsnNode.LABEL
                || p.getType() == AbstractInsnNode.LINE
                || p.getType() == AbstractInsnNode.FRAME)) {
            p = p.getPrevious();
        }
        return p;
    }

    private static final class HandlerInsn {
        private HandlerInsn() {
        }

        static boolean isTerminator(int op) {
            if (op < 0) {
                return false;
            }
            return switch (op) {
                case Opcodes.ATHROW,
                     Opcodes.RETURN, Opcodes.IRETURN, Opcodes.LRETURN,
                     Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN -> true;
                default -> false;
            };
        }
    }
}
