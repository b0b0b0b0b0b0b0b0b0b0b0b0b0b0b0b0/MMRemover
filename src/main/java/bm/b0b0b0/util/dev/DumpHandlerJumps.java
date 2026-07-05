package bm.b0b0b0.util.dev;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;

public final class DumpHandlerJumps {
    public static void main(String[] args) throws Exception {
        String jar = args.length > 0 ? args[0] : "input/Multiverse-Core-4.3.1.jar";
        String cls = args.length > 1 ? args[1] : "com/onarandombox/MultiverseCore/MultiverseCore.class";
        String method = args.length > 2 ? args[2] : "loadConfigs";

        byte[] raw;
        try (ZipFile z = new ZipFile(Path.of(jar).toFile())) {
            raw = z.getInputStream(z.getEntry(cls)).readAllBytes();
        }
        ClassNode cn = new ClassNode();
        new ClassReader(raw).accept(cn, ClassReader.SKIP_DEBUG);

        for (MethodNode m : cn.methods) {
            if (!m.name.equals(method)) continue;
            System.out.println("Method " + m.name + m.desc + " maxLocals=" + m.maxLocals);
            Map<LabelNode, List<String>> jumps = new HashMap<>();
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof JumpInsnNode j) {
                    jumps.computeIfAbsent(j.label, k -> new ArrayList<>())
                            .add(j.getOpcode() + " @" + insnIndex(m, insn));
                } else if (insn instanceof TableSwitchInsnNode ts) {
                    for (LabelNode l : ts.labels) {
                        jumps.computeIfAbsent(l, k -> new ArrayList<>())
                                .add("tableswitch @" + insnIndex(m, insn));
                    }
                    jumps.computeIfAbsent(ts.dflt, k -> new ArrayList<>())
                            .add("tableswitch-default @" + insnIndex(m, insn));
                } else if (insn instanceof LookupSwitchInsnNode ls) {
                    for (LabelNode l : ls.labels) {
                        jumps.computeIfAbsent(l, k -> new ArrayList<>())
                                .add("lookupswitch @" + insnIndex(m, insn));
                    }
                    jumps.computeIfAbsent(ls.dflt, k -> new ArrayList<>())
                            .add("lookupswitch-default @" + insnIndex(m, insn));
                }
            }
            int i = 0;
            for (TryCatchBlockNode tcb : m.tryCatchBlocks) {
                String type = tcb.type == null ? "null" : tcb.type;
                System.out.printf("tcb[%d] %d..%d -> handler@%d type=%s%n",
                        i++, insnIndex(m, tcb.start), insnIndex(m, tcb.end),
                        insnIndex(m, tcb.handler), type);
                List<String> inc = jumps.getOrDefault(tcb.handler, List.of());
                if (!inc.isEmpty()) {
                    System.out.println("  goto/jsr to handler: " + inc);
                }
            }
            break;
        }
    }

    static int insnIndex(MethodNode m, AbstractInsnNode target) {
        int i = 0;
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn == target) return i;
            i++;
        }
        return -1;
    }
}
