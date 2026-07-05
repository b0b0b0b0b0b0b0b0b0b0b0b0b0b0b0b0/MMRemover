package bm.b0b0b0.util.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
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
import java.util.List;
import java.util.Map;

public final class CaseInsensitiveClassRefFixer {

    private CaseInsensitiveClassRefFixer() {
    }

    public static Map<String, String> canonicalNames(List<String> classEntryNames) {
        Map<String, String> byLower = new HashMap<>();
        for (String name : classEntryNames) {
            if (!name.endsWith(".class")) {
                continue;
            }
            String internal = name.substring(0, name.length() - ".class".length());
            byLower.putIfAbsent(internal.toLowerCase(), internal);
        }
        return byLower;
    }

    public static byte[] fix(byte[] clazz, Map<String, String> canonicalByLowercase) {
        if (clazz == null || clazz.length == 0 || canonicalByLowercase.isEmpty()) {
            return clazz;
        }
        ClassReader cr = new ClassReader(clazz);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        boolean changed = fix(cn, canonicalByLowercase);
        if (!changed) {
            return clazz;
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    public static boolean fix(ClassNode cn, Map<String, String> canonicalByLowercase) {
        boolean changed = false;
        String superFixed = resolve(cn.superName, canonicalByLowercase);
        if (cn.superName != null && !superFixed.equals(cn.superName)) {
            cn.superName = superFixed;
            changed = true;
        }
        if (cn.interfaces != null) {
            for (int i = 0; i < cn.interfaces.size(); i++) {
                String fixed = resolve(cn.interfaces.get(i), canonicalByLowercase);
                if (!fixed.equals(cn.interfaces.get(i))) {
                    cn.interfaces.set(i, fixed);
                    changed = true;
                }
            }
        }
        for (MethodNode method : cn.methods) {
            String fixedDesc = fixDescriptor(method.desc, canonicalByLowercase);
            if (!fixedDesc.equals(method.desc)) {
                method.desc = fixedDesc;
                changed = true;
            }
            for (AbstractInsnNode insn : method.instructions) {
                changed |= fixInsn(insn, canonicalByLowercase);
            }
        }
        for (FieldNode field : cn.fields) {
            String fixedDesc = fixDescriptor(field.desc, canonicalByLowercase);
            if (!fixedDesc.equals(field.desc)) {
                field.desc = fixedDesc;
                changed = true;
            }
        }
        return changed;
    }

    private static boolean fixInsn(AbstractInsnNode insn, Map<String, String> canonicalByLowercase) {
        if (insn instanceof TypeInsnNode typeInsn) {
            String fixed = resolve(typeInsn.desc, canonicalByLowercase);
            if (!fixed.equals(typeInsn.desc)) {
                typeInsn.desc = fixed;
                return true;
            }
            return false;
        }
        if (insn instanceof FieldInsnNode fieldInsn) {
            boolean changed = false;
            String owner = resolve(fieldInsn.owner, canonicalByLowercase);
            if (!owner.equals(fieldInsn.owner)) {
                fieldInsn.owner = owner;
                changed = true;
            }
            String desc = fixDescriptor(fieldInsn.desc, canonicalByLowercase);
            if (!desc.equals(fieldInsn.desc)) {
                fieldInsn.desc = desc;
                changed = true;
            }
            return changed;
        }
        if (insn instanceof MethodInsnNode methodInsn) {
            boolean changed = false;
            String owner = resolve(methodInsn.owner, canonicalByLowercase);
            if (!owner.equals(methodInsn.owner)) {
                methodInsn.owner = owner;
                changed = true;
            }
            String desc = fixDescriptor(methodInsn.desc, canonicalByLowercase);
            if (!desc.equals(methodInsn.desc)) {
                methodInsn.desc = desc;
                changed = true;
            }
            return changed;
        }
        if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type type) {
            String fixed = resolve(type.getInternalName(), canonicalByLowercase);
            if (!fixed.equals(type.getInternalName())) {
                ldc.cst = Type.getObjectType(fixed);
                return true;
            }
        }
        if (insn instanceof InvokeDynamicInsnNode indy) {
            boolean changed = false;
            String fixedIndyDesc = fixDescriptor(indy.desc, canonicalByLowercase);
            if (!fixedIndyDesc.equals(indy.desc)) {
                indy.desc = fixedIndyDesc;
                changed = true;
            }
            if (indy.bsmArgs != null) {
                for (int i = 0; i < indy.bsmArgs.length; i++) {
                    Object arg = indy.bsmArgs[i];
                    if (arg instanceof Type type) {
                        String fixed = resolve(type.getInternalName(), canonicalByLowercase);
                        if (!fixed.equals(type.getInternalName())) {
                            indy.bsmArgs[i] = Type.getObjectType(fixed);
                            changed = true;
                        }
                    } else if (arg instanceof Handle handle) {
                        String owner = resolve(handle.getOwner(), canonicalByLowercase);
                        String desc = fixDescriptor(handle.getDesc(), canonicalByLowercase);
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

    private static String fixDescriptor(String desc, Map<String, String> canonicalByLowercase) {
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
                String fixed = resolve(internal, canonicalByLowercase);
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

    private static String resolve(String internal, Map<String, String> canonicalByLowercase) {
        if (internal == null || internal.isEmpty()) {
            return internal;
        }
        String canonical = canonicalByLowercase.get(internal.toLowerCase());
        if (canonical == null || canonical.equals(internal)) {
            return internal;
        }
        return canonical;
    }
}
