package bm.b0b0b0.util.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CaseInsensitiveClassRefFixer {

    private CaseInsensitiveClassRefFixer() {
    }

    public static final class Context {
        private final Set<String> exactNames;
        private final Map<String, String> unambiguousByLowercase;

        private Context(Set<String> exactNames, Map<String, String> unambiguousByLowercase) {
            this.exactNames = exactNames;
            this.unambiguousByLowercase = unambiguousByLowercase;
        }

        public static Context of(List<String> classEntryNames) {
            Map<String, Integer> counts = new HashMap<>();
            Map<String, String> firstByLower = new HashMap<>();
            Set<String> exact = new HashSet<>();
            for (String name : classEntryNames) {
                if (!name.endsWith(".class")) {
                    continue;
                }
                String internal = name.substring(0, name.length() - ".class".length());
                exact.add(internal);
                String lower = internal.toLowerCase();
                counts.merge(lower, 1, Integer::sum);
                firstByLower.putIfAbsent(lower, internal);
            }
            Map<String, String> unambiguous = new HashMap<>();
            for (Map.Entry<String, String> entry : firstByLower.entrySet()) {
                if (counts.get(entry.getKey()) == 1) {
                    unambiguous.put(entry.getKey(), entry.getValue());
                }
            }
            return new Context(exact, unambiguous);
        }

        public boolean isEmpty() {
            return unambiguousByLowercase.isEmpty();
        }

        private String resolve(String internal) {
            if (internal == null || internal.isEmpty()) {
                return internal;
            }
            if (exactNames.contains(internal)) {
                return internal;
            }
            String canonical = unambiguousByLowercase.get(internal.toLowerCase());
            return canonical != null ? canonical : internal;
        }
    }

    public static byte[] fix(byte[] clazz, Context context, Map<String, byte[]> jarClasses) {
        if (clazz == null || clazz.length == 0 || context == null || context.isEmpty()) {
            return clazz;
        }
        ClassReader cr = new ClassReader(clazz);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        boolean changed = fix(cn, context);
        if (!changed) {
            return clazz;
        }
        try {
            return JarClassWriter.writeModified(cn, jarClasses, Set.of());
        } catch (Throwable primary) {
            try {
                return JarClassWriter.emitComputed(cr, cn, jarClasses);
            } catch (Throwable secondary) {
                return clazz;
            }
        }
    }

    public static boolean fix(ClassNode cn, Context context) {
        boolean changed = false;
        String superFixed = context.resolve(cn.superName);
        if (cn.superName != null && !superFixed.equals(cn.superName)) {
            cn.superName = superFixed;
            changed = true;
        }
        if (cn.interfaces != null) {
            for (int i = 0; i < cn.interfaces.size(); i++) {
                String fixed = context.resolve(cn.interfaces.get(i));
                if (!fixed.equals(cn.interfaces.get(i))) {
                    cn.interfaces.set(i, fixed);
                    changed = true;
                }
            }
        }
        for (MethodNode method : cn.methods) {
            String fixedDesc = fixDescriptor(method.desc, context);
            if (!fixedDesc.equals(method.desc)) {
                method.desc = fixedDesc;
                changed = true;
            }
            for (AbstractInsnNode insn : method.instructions) {
                changed |= fixInsn(insn, context);
            }
        }
        for (FieldNode field : cn.fields) {
            String fixedDesc = fixDescriptor(field.desc, context);
            if (!fixedDesc.equals(field.desc)) {
                field.desc = fixedDesc;
                changed = true;
            }
        }
        return changed;
    }

    private static boolean fixInsn(AbstractInsnNode insn, Context context) {
        if (insn instanceof TypeInsnNode typeInsn) {
            String fixed = context.resolve(typeInsn.desc);
            if (!fixed.equals(typeInsn.desc)) {
                typeInsn.desc = fixed;
                return true;
            }
            return false;
        }
        if (insn instanceof FieldInsnNode fieldInsn) {
            boolean changed = false;
            String owner = context.resolve(fieldInsn.owner);
            if (!owner.equals(fieldInsn.owner)) {
                fieldInsn.owner = owner;
                changed = true;
            }
            String desc = fixDescriptor(fieldInsn.desc, context);
            if (!desc.equals(fieldInsn.desc)) {
                fieldInsn.desc = desc;
                changed = true;
            }
            return changed;
        }
        if (insn instanceof MethodInsnNode methodInsn) {
            boolean changed = false;
            String owner = context.resolve(methodInsn.owner);
            if (!owner.equals(methodInsn.owner)) {
                methodInsn.owner = owner;
                changed = true;
            }
            String desc = fixDescriptor(methodInsn.desc, context);
            if (!desc.equals(methodInsn.desc)) {
                methodInsn.desc = desc;
                changed = true;
            }
            return changed;
        }
        if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type type) {
            String fixed = context.resolve(type.getInternalName());
            if (!fixed.equals(type.getInternalName())) {
                ldc.cst = Type.getObjectType(fixed);
                return true;
            }
        }
        if (insn instanceof InvokeDynamicInsnNode indy) {
            boolean changed = false;
            String fixedIndyDesc = fixDescriptor(indy.desc, context);
            if (!fixedIndyDesc.equals(indy.desc)) {
                indy.desc = fixedIndyDesc;
                changed = true;
            }
            if (indy.bsmArgs != null) {
                for (int i = 0; i < indy.bsmArgs.length; i++) {
                    Object arg = indy.bsmArgs[i];
                    if (arg instanceof Type type) {
                        String fixed = context.resolve(type.getInternalName());
                        if (!fixed.equals(type.getInternalName())) {
                            indy.bsmArgs[i] = Type.getObjectType(fixed);
                            changed = true;
                        }
                    } else if (arg instanceof Handle handle) {
                        String owner = context.resolve(handle.getOwner());
                        String desc = fixDescriptor(handle.getDesc(), context);
                        if (!owner.equals(handle.getOwner()) || !desc.equals(handle.getDesc())) {
                            indy.bsmArgs[i] = new Handle(handle.getTag(), owner, handle.getName(), desc, handle.isInterface());
                            changed = true;
                        }
                    }
                }
            }
            return changed;
        }
        return false;
    }

    private static String fixDescriptor(String desc, Context context) {
        if (desc == null || desc.isEmpty()) {
            return desc;
        }
        StringBuilder out = new StringBuilder(desc.length());
        int i = 0;
        boolean changed = false;
        while (i < desc.length()) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int semi = desc.indexOf(';', i);
                if (semi < 0) {
                    out.append(desc.substring(i));
                    break;
                }
                String internal = desc.substring(i + 1, semi);
                String fixed = context.resolve(internal);
                changed |= !fixed.equals(internal);
                out.append('L').append(fixed).append(';');
                i = semi + 1;
                continue;
            }
            out.append(c);
            i++;
        }
        return changed ? out.toString() : desc;
    }
}
