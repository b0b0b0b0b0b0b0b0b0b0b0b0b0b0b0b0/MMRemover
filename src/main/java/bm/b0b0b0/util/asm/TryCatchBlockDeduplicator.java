package bm.b0b0b0.util.asm;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class TryCatchBlockDeduplicator {

    private TryCatchBlockDeduplicator() {
    }

    public static void dedupe(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            dedupe(method);
        }
    }

    public static void dedupe(MethodNode method) {
        List<TryCatchBlockNode> blocks = method.tryCatchBlocks;
        if (blocks == null || blocks.size() < 2) {
            return;
        }
        Set<String> seen = new HashSet<>();
        Iterator<TryCatchBlockNode> it = blocks.iterator();
        while (it.hasNext()) {
            TryCatchBlockNode block = it.next();
            String key = key(block);
            if (!seen.add(key)) {
                it.remove();
            }
        }
    }

    private static String key(TryCatchBlockNode block) {
        return labelId(block.start) + '|' + labelId(block.end) + '|' + labelId(block.handler) + '|' + typeKey(block);
    }

    private static String labelId(LabelNode label) {
        return label == null ? "null" : Integer.toHexString(System.identityHashCode(label));
    }

    private static String typeKey(TryCatchBlockNode block) {
        if (block.type == null) {
            return "all";
        }
        return block.type;
    }
}
