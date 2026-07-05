package bm.b0b0b0.util.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FrameRecomputer {

    private FrameRecomputer() {
    }

    public static void recompute(ClassNode classNode, Map<String, byte[]> jarClasses) {
        ClassLoader loader = new JarBytecodeClassLoader(jarClasses, FrameRecomputer.class.getClassLoader());
        Type currentType = Type.getObjectType(classNode.name);
        Type superType = classNode.superName != null
                ? Type.getObjectType(classNode.superName)
                : Type.getType("Ljava/lang/Object;");
        Type[] interfaces = classNode.interfaces.stream().map(Type::getObjectType).toArray(Type[]::new);
        SimpleVerifier verifier = new JarVerifier(currentType, superType, interfaces, loader);

        for (MethodNode method : classNode.methods) {
            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                continue;
            }
            if (method.instructions == null || method.instructions.size() == 0) {
                continue;
            }
            recomputeMethod(classNode.name, method, verifier);
        }
    }

    public static void recomputeOne(ClassNode owner, MethodNode method, Map<String, byte[]> jarClasses) {
        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
            return;
        }
        if (method.instructions == null || method.instructions.size() == 0) {
            return;
        }
        ClassLoader loader = new JarBytecodeClassLoader(jarClasses, FrameRecomputer.class.getClassLoader());
        Type superType = owner.superName != null
                ? Type.getObjectType(owner.superName)
                : Type.getType("Ljava/lang/Object;");
        Type[] interfaces = owner.interfaces.stream().map(Type::getObjectType).toArray(Type[]::new);
        SimpleVerifier verifier = new JarVerifier(
                Type.getObjectType(owner.name),
                superType,
                interfaces,
                loader);
        recomputeMethod(owner.name, method, verifier);
    }

    private static void recomputeMethod(String owner, MethodNode method, SimpleVerifier verifier) {
        removeFrames(method);
        method.localVariables = null;

        Analyzer<BasicValue> analyzer = new Analyzer<>(verifier);
        Frame<BasicValue>[] frames;
        try {
            frames = analyzer.analyze(owner, method);
        } catch (AnalyzerException e) {
            throw new IllegalStateException("Failed to analyze " + owner + "." + method.name + method.desc, e);
        }

        Set<AbstractInsnNode> frameTargets = collectFrameTargets(method);
        AbstractInsnNode[] insns = method.instructions.toArray();
        for (int i = 0; i < insns.length; i++) {
            AbstractInsnNode insn = insns[i];
            if (!frameTargets.contains(insn)) {
                continue;
            }
            Frame<BasicValue> frame = frames[i];
            if (frame == null) {
                continue;
            }
            method.instructions.insertBefore(insn, toFrameNode(frame));
        }
    }

    public static void stripFrames(MethodNode method) {
        removeFrames(method);
    }

    private static void removeFrames(MethodNode method) {
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof FrameNode) {
                method.instructions.remove(insn);
            }
        }
    }

    private static Set<AbstractInsnNode> collectFrameTargets(MethodNode method) {
        Set<AbstractInsnNode> targets = new HashSet<>();
        AbstractInsnNode insn = method.instructions.getFirst();
        while (insn != null) {
            if (insn instanceof LabelNode) {
                AbstractInsnNode next = nextRealInsn(insn);
                if (next != null) {
                    targets.add(next);
                }
            }
            insn = insn.getNext();
        }

        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node instanceof JumpInsnNode jump) {
                addTarget(targets, jump.label);
            } else if (node instanceof TableSwitchInsnNode tableSwitch) {
                addTarget(targets, tableSwitch.dflt);
                for (LabelNode label : tableSwitch.labels) {
                    addTarget(targets, label);
                }
            } else if (node instanceof LookupSwitchInsnNode lookupSwitch) {
                addTarget(targets, lookupSwitch.dflt);
                for (LabelNode label : lookupSwitch.labels) {
                    addTarget(targets, label);
                }
            }
        }

        if (method.tryCatchBlocks != null) {
            for (TryCatchBlockNode block : method.tryCatchBlocks) {
                addTarget(targets, block.handler);
            }
        }
        return targets;
    }

    private static void addTarget(Set<AbstractInsnNode> targets, LabelNode label) {
        AbstractInsnNode next = nextRealInsn(label);
        if (next != null) {
            targets.add(next);
        }
    }

    private static AbstractInsnNode nextRealInsn(AbstractInsnNode node) {
        AbstractInsnNode current = node instanceof LabelNode ? node.getNext() : node;
        while (current instanceof LabelNode || current instanceof LineNumberNode || current instanceof FrameNode) {
            current = current.getNext();
        }
        return current;
    }

    private static FrameNode toFrameNode(Frame<BasicValue> frame) {
        int locals = frame.getLocals();
        Object[] localTypes = new Object[locals];
        for (int i = 0; i < locals; i++) {
            localTypes[i] = toFrameType(frame.getLocal(i));
        }

        int stackSize = frame.getStackSize();
        Object[] stackTypes = new Object[stackSize];
        for (int i = 0; i < stackSize; i++) {
            stackTypes[i] = toFrameType(frame.getStack(i));
        }

        return new FrameNode(Opcodes.F_NEW, locals, localTypes, stackSize, stackTypes);
    }

    private static Object toFrameType(BasicValue value) {
        if (value == null || value == BasicValue.UNINITIALIZED_VALUE) {
            return Opcodes.TOP;
        }
        if (value == BasicValue.INT_VALUE) {
            return Opcodes.INTEGER;
        }
        if (value == BasicValue.FLOAT_VALUE) {
            return Opcodes.FLOAT;
        }
        if (value == BasicValue.LONG_VALUE) {
            return Opcodes.LONG;
        }
        if (value == BasicValue.DOUBLE_VALUE) {
            return Opcodes.DOUBLE;
        }
        if (value == BasicValue.RETURNADDRESS_VALUE) {
            return Opcodes.TOP;
        }

        Type type = value.getType();
        if (type == null) {
            return Opcodes.TOP;
        }
        return switch (type.getSort()) {
            case Type.OBJECT -> type.getInternalName();
            case Type.ARRAY -> type.getDescriptor();
            case Type.INT -> Opcodes.INTEGER;
            case Type.FLOAT -> Opcodes.FLOAT;
            case Type.LONG -> Opcodes.LONG;
            case Type.DOUBLE -> Opcodes.DOUBLE;
            case Type.BYTE -> Opcodes.INTEGER;
            case Type.CHAR -> Opcodes.INTEGER;
            case Type.SHORT -> Opcodes.INTEGER;
            case Type.BOOLEAN -> Opcodes.INTEGER;
            default -> Opcodes.TOP;
        };
    }

    private static final class JarVerifier extends SimpleVerifier {

        private final ClassLoader loader;

        private JarVerifier(Type currentClass, Type superClass, Type[] interfaces, ClassLoader loader) {
            super(currentClass, superClass, Arrays.asList(interfaces), false);
            this.loader = loader;
        }

        @Override
        protected Class<?> getClass(Type type) {
            if (type.getSort() == Type.OBJECT) {
                try {
                    return Class.forName(type.getClassName(), false, loader);
                } catch (ClassNotFoundException ignored) {
                    return null;
                }
            }
            return super.getClass(type);
        }
    }
}
