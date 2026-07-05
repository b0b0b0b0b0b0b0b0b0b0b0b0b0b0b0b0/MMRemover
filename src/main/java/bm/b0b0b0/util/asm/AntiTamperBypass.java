package bm.b0b0b0.util.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AntiTamperBypass {

    private static final String HASH_ERROR = "Error in hash";
    private static final Set<String> TAMPER_TYPES = Set.of(
            "java/io/IOException",
            "java/lang/IllegalAccessException",
            "java/lang/RuntimeException"
    );

    private AntiTamperBypass() {
    }

    public static boolean containsHashError(byte[] clazz) {
        if (clazz == null || clazz.length == 0) {
            return false;
        }
        return new String(clazz, StandardCharsets.ISO_8859_1).contains(HASH_ERROR);
    }

    public static boolean mayNeedBypass(byte[] clazz) {
        if (clazz == null || clazz.length == 0) {
            return false;
        }
        return containsHashError(clazz);
    }

    public static byte[] bypass(byte[] clazz, Map<String, byte[]> jarClasses) {
        if (clazz == null || clazz.length == 0 || !mayNeedBypass(clazz)) {
            return clazz;
        }
        ClassReader cr = new ClassReader(clazz);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        Set<String> touchedMethods = new HashSet<>();
        boolean changed = false;
        boolean flowObfuscated = FlowObfuscationDetector.isFlowObfuscated(clazz);
        for (MethodNode method : cn.methods) {
            if (bypassMethod(method)) {
                touchedMethods.add(method.name + method.desc);
                changed = true;
            }
            if (!flowObfuscated && removeOrphanAthrow(method)) {
                touchedMethods.add(method.name + method.desc);
                changed = true;
            }
        }
        if (!changed) {
            return clazz;
        }
        try {
            return JarClassWriter.writeModified(cn, jarClasses, touchedMethods, false);
        } catch (Throwable primary) {
            try {
                return JarClassWriter.writePatchedMethods(cr, cn, jarClasses, touchedMethods, false);
            } catch (Throwable secondary) {
                try {
                    return JarClassWriter.writePreserving(cr, cn);
                } catch (Throwable tertiary) {
                    try {
                        return JarClassWriter.emitComputed(cr, cn, jarClasses);
                    } catch (Throwable quaternary) {
                        return clazz;
                    }
                }
            }
        }
    }

    private static boolean bypassMethod(MethodNode method) {
        boolean changed = false;
        List<AbstractInsnNode> snapshot = new ArrayList<>();
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            snapshot.add(insn);
        }
        for (AbstractInsnNode insn : snapshot) {
            if (!(insn instanceof InsnNode throwInsn) || throwInsn.getOpcode() != Opcodes.ATHROW) {
                continue;
            }
            TamperThrow match = parseTamperThrow(throwInsn);
            if (match == null) {
                continue;
            }
            LabelNode target = new LabelNode();
            AbstractInsnNode after = realNext(throwInsn);
            if (after == null) {
                method.instructions.add(target);
                method.instructions.add(new InsnNode(returnOpcode(method)));
            } else {
                method.instructions.insert(throwInsn, target);
            }
            method.instructions.insertBefore(match.start, new JumpInsnNode(Opcodes.GOTO, target));
            for (AbstractInsnNode node : match.nodes) {
                method.instructions.remove(node);
            }
            changed = true;
        }
        return changed;
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

    private static boolean removeOrphanAthrow(MethodNode method) {
        boolean changed = false;
        List<AbstractInsnNode> snapshot = new ArrayList<>();
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            snapshot.add(insn);
        }
        for (AbstractInsnNode insn : snapshot) {
            if (!(insn instanceof InsnNode throwInsn) || throwInsn.getOpcode() != Opcodes.ATHROW) {
                continue;
            }
            if (parseTamperThrow(throwInsn) != null) {
                continue;
            }
            AbstractInsnNode prev = realPrevious(throwInsn);
            if (prev instanceof MethodInsnNode call
                    && call.getOpcode() == Opcodes.INVOKESPECIAL
                    && "<init>".equals(call.name)
                    && TAMPER_TYPES.contains(call.owner)) {
                continue;
            }
            method.instructions.remove(throwInsn);
            changed = true;
        }
        return changed;
    }

    private static TamperThrow parseTamperThrow(AbstractInsnNode athrow) {
        AbstractInsnNode init = realPrevious(athrow);
        if (!(init instanceof MethodInsnNode initCall)
                || initCall.getOpcode() != Opcodes.INVOKESPECIAL
                || !"<init>".equals(initCall.name)
                || !TAMPER_TYPES.contains(initCall.owner)) {
            return null;
        }

        AbstractInsnNode beforeInit = realPrevious(init);
        LdcInsnNode hashLdc = null;
        AbstractInsnNode dup;
        if (beforeInit instanceof LdcInsnNode ldc && HASH_ERROR.equals(ldc.cst)) {
            if (!"(Ljava/lang/String;)V".equals(initCall.desc)) {
                return null;
            }
            hashLdc = ldc;
            dup = realPrevious(beforeInit);
        } else {
            if (!"()V".equals(initCall.desc)) {
                return null;
            }
            dup = beforeInit;
        }

        if (!(dup instanceof InsnNode dupInsn) || dupInsn.getOpcode() != Opcodes.DUP) {
            return null;
        }

        AbstractInsnNode beforeDup = realPrevious(dup);
        if (!(beforeDup instanceof TypeInsnNode newInsn) || newInsn.getOpcode() != Opcodes.NEW) {
            return null;
        }
        if (!initCall.owner.equals(newInsn.desc)) {
            return null;
        }

        List<AbstractInsnNode> nodes = new ArrayList<>();
        nodes.add(beforeDup);
        nodes.add(dup);
        if (hashLdc != null) {
            nodes.add(hashLdc);
        }
        nodes.add(init);
        nodes.add(athrow);
        return new TamperThrow(beforeDup, nodes);
    }

    private static AbstractInsnNode realPrevious(AbstractInsnNode insn) {
        AbstractInsnNode cur = insn.getPrevious();
        while (cur != null && isMarker(cur)) {
            cur = cur.getPrevious();
        }
        return cur;
    }

    private static AbstractInsnNode realNext(AbstractInsnNode insn) {
        AbstractInsnNode cur = insn.getNext();
        while (cur != null && isMarker(cur)) {
            cur = cur.getNext();
        }
        return cur;
    }

    private static boolean isMarker(AbstractInsnNode insn) {
        return insn instanceof LabelNode
                || insn instanceof LineNumberNode
                || insn instanceof FrameNode;
    }

    private record TamperThrow(AbstractInsnNode start, List<AbstractInsnNode> nodes) {
    }
}
