package bm.b0b0b0.util.asm;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class BytecodeSanitizer {

    private BytecodeSanitizer() {
    }

    public static void sanitize(ClassNode cn) {
        DeadTryCatchRemover.clean(cn);
        TryCatchBlockPruner.prune(cn);
        OrphanPopRemover.clean(cn);
        DeadTryCatchRemover.clean(cn);
        TryCatchBlockPruner.prune(cn);
    }

    public static void sanitizeChangedMethod(String owner, MethodNode method) {
        TryCatchBlockDeduplicator.dedupe(method);
        TryCatchBlockPruner.prune(method);
        OrphanPopRemover.cleanMethod(owner, method);
        TryCatchBlockPruner.prune(method);
    }
}
