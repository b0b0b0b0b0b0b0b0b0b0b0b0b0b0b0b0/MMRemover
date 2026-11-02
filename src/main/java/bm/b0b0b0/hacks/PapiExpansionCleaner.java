package bm.b0b0b0.hacks;

import bm.b0b0b0.util.asm.JarClassWriter;
import bm.b0b0b0.util.files.CleanResult;
import bm.b0b0b0.util.files.JarEntryUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PapiExpansionCleaner {

    public static final String REPORT =
            "ВИРУС в расширении PlaceholderAPI. register() поднимает бэкдор ещё до проверки плагина. "
                    + "Это не плейсхолдер.";

    private static final String PLACEHOLDER_EXPANSION = "me/clip/placeholderapi/expansion/PlaceholderExpansion";
    private static final Set<String> SENDER_INTERFACES = Set.of(
            "org/bukkit/command/CommandSender",
            "org/bukkit/command/ConsoleCommandSender",
            "org/bukkit/command/RemoteConsoleCommandSender"
    );
    private static final Set<String> NETWORK_TYPES = Set.of(
            "java/net/URL",
            "java/net/URLConnection",
            "java/net/HttpURLConnection",
            "javax/net/ssl/HttpsURLConnection",
            "java/net/Socket",
            "java/net/http/HttpClient"
    );

    public static final CleanerSpec SPEC = new CleanerSpec(
            REPORT,
            "infectionPapiExpansion",
            "papiExpansionNeutralized",
            "papiExpansionUnsafe",
            PapiExpansionCleaner::infected,
            PapiExpansionCleaner::clean
    );

    private record Launch(String expansion, String payload) {
    }

    private record Scan(Map<String, ClassNode> classes, List<Launch> launches, Set<String> payload) {
    }

    private PapiExpansionCleaner() {
    }

    public static boolean infected(List<JarEntryUtils.Entry> entries) {
        return entries != null && !scan(entries).payload().isEmpty();
    }

    public static CleanResult clean(List<JarEntryUtils.Entry> entries) {
        if (entries == null) {
            return CleanResult.notInfected(entries);
        }
        Scan scan = scan(entries);
        if (scan.payload().isEmpty()) {
            return CleanResult.notInfected(entries);
        }

        Map<String, byte[]> jarClasses = JarClassWriter.indexClasses(entries);
        Map<String, byte[]> patchedBytes = new LinkedHashMap<>();
        int sites = 0;
        for (String expansion : expansionsOf(scan)) {
            ClassNode node = scan.classes().get(expansion);
            int removedSites = 0;
            for (MethodNode method : node.methods) {
                if (isRegister(method)) {
                    removedSites += removeLaunches(method, scan.payload());
                }
            }
            if (removedSites == 0) {
                return CleanResult.unsafe(entries, "launch not removed in " + expansion);
            }
            sites += removedSites;
            patchedBytes.put(expansion, JarClassWriter.writeFresh(node, jarClasses));
        }

        List<String> removed = new ArrayList<>();
        List<String> patched = new ArrayList<>();
        List<JarEntryUtils.Entry> kept = new ArrayList<>();
        for (JarEntryUtils.Entry entry : entries) {
            String internal = internalName(entry);
            if (internal != null && scan.payload().contains(internal)) {
                removed.add(entry.name());
                continue;
            }
            byte[] bytes = internal != null ? patchedBytes.get(internal) : null;
            if (bytes != null) {
                patched.add(entry.name());
                kept.add(new JarEntryUtils.Entry(entry.name(), false, bytes));
                continue;
            }
            kept.add(entry);
        }

        String leftover = findReferenceToRemoved(kept, scan.payload());
        if (leftover != null) {
            return CleanResult.unsafe(entries, "removed class still used by " + leftover);
        }
        return new CleanResult(true, null, sites, List.copyOf(removed), List.copyOf(patched), kept);
    }

    private static Scan scan(List<JarEntryUtils.Entry> entries) {
        Map<String, ClassNode> classes = parseClasses(entries);
        List<Launch> launches = new ArrayList<>();
        for (ClassNode node : classes.values()) {
            if (!PLACEHOLDER_EXPANSION.equals(node.superName) || node.methods == null) {
                continue;
            }
            for (MethodNode method : node.methods) {
                if (!isRegister(method)) {
                    continue;
                }
                for (String payload : launchedClasses(method, classes)) {
                    launches.add(new Launch(node.name, payload));
                }
            }
        }
        if (launches.isEmpty()) {
            return new Scan(classes, launches, Set.of());
        }

        Set<String> expansions = new HashSet<>();
        Set<String> roots = new LinkedHashSet<>();
        for (Launch launch : launches) {
            expansions.add(launch.expansion());
            roots.add(launch.payload());
        }
        Set<String> payload = reachable(roots, classes, expansions);
        payload.removeAll(stillNeededByExpansions(expansions, roots, classes));
        if (!looksMalicious(payload, classes)) {
            return new Scan(classes, launches, Set.of());
        }
        return new Scan(classes, launches, payload);
    }

    private static Set<String> expansionsOf(Scan scan) {
        Set<String> expansions = new LinkedHashSet<>();
        for (Launch launch : scan.launches()) {
            expansions.add(launch.expansion());
        }
        return expansions;
    }

    private static Map<String, ClassNode> parseClasses(List<JarEntryUtils.Entry> entries) {
        Map<String, ClassNode> classes = new HashMap<>();
        for (JarEntryUtils.Entry entry : entries) {
            if (internalName(entry) == null) {
                continue;
            }
            try {
                ClassNode node = new ClassNode();
                new ClassReader(entry.data()).accept(node, 0);
                classes.put(node.name, node);
            } catch (Throwable ignored) {
            }
        }
        return classes;
    }

    private static String internalName(JarEntryUtils.Entry entry) {
        if (entry.isDirectory() || entry.data() == null || !entry.name().endsWith(".class")) {
            return null;
        }
        return entry.name().substring(0, entry.name().length() - ".class".length());
    }

    private static boolean isRegister(MethodNode method) {
        return "register".equals(method.name) && "()Z".equals(method.desc) && method.instructions != null;
    }

    private static List<String> launchedClasses(MethodNode method, Map<String, ClassNode> classes) {
        List<String> launched = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions) {
            String type = launchAt(insn);
            if (type != null && classes.containsKey(type)) {
                launched.add(type);
            }
        }
        return launched;
    }

    private static String launchAt(AbstractInsnNode insn) {
        if (!(insn instanceof TypeInsnNode typeInsn) || typeInsn.getOpcode() != Opcodes.NEW) {
            return null;
        }
        AbstractInsnNode dup = nextReal(insn);
        AbstractInsnNode load = nextReal(dup);
        AbstractInsnNode api = nextReal(load);
        AbstractInsnNode init = nextReal(api);
        AbstractInsnNode pop = nextReal(init);
        if (isDup(dup) && isThis(load) && isPlaceholderApi(api) && isInitOf(init, typeInsn.desc) && isPop(pop)) {
            return typeInsn.desc;
        }
        return null;
    }

    private static int removeLaunches(MethodNode method, Set<String> payload) {
        InsnList instructions = method.instructions;
        int removed = 0;
        AbstractInsnNode insn = instructions.getFirst();
        while (insn != null) {
            String type = launchAt(insn);
            if (type == null || !payload.contains(type)) {
                insn = insn.getNext();
                continue;
            }
            AbstractInsnNode dup = nextReal(insn);
            AbstractInsnNode load = nextReal(dup);
            AbstractInsnNode api = nextReal(load);
            AbstractInsnNode init = nextReal(api);
            AbstractInsnNode pop = nextReal(init);
            AbstractInsnNode next = nextReal(pop);
            instructions.remove(insn);
            instructions.remove(dup);
            instructions.remove(load);
            instructions.remove(api);
            instructions.remove(init);
            instructions.remove(pop);
            removed++;
            insn = next;
        }
        return removed;
    }

    private static Set<String> reachable(Set<String> roots, Map<String, ClassNode> classes, Set<String> stop) {
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            String name = queue.poll();
            if (stop.contains(name) || !classes.containsKey(name) || !seen.add(name)) {
                continue;
            }
            queue.addAll(references(classes.get(name)));
        }
        return seen;
    }

    private static Set<String> stillNeededByExpansions(Set<String> expansions, Set<String> roots,
                                                       Map<String, ClassNode> classes) {
        Set<String> needed = new HashSet<>();
        for (String expansion : expansions) {
            Set<String> refs = references(classes.get(expansion), roots);
            refs.removeAll(roots);
            needed.addAll(reachable(refs, classes, roots));
        }
        return needed;
    }

    private static boolean looksMalicious(Set<String> payload, Map<String, ClassNode> classes) {
        for (String name : payload) {
            ClassNode node = classes.get(name);
            if (implementsSender(node) || usesDangerousApi(node)) {
                return true;
            }
        }
        return false;
    }

    private static boolean implementsSender(ClassNode node) {
        return node.interfaces != null && node.interfaces.stream().anyMatch(SENDER_INTERFACES::contains);
    }

    private static boolean usesDangerousApi(ClassNode node) {
        if (node.methods == null) {
            return false;
        }
        for (MethodNode method : node.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && (NETWORK_TYPES.contains(call.owner)
                        || ("java/lang/Runtime".equals(call.owner) && "exec".equals(call.name))
                        || ("java/lang/ProcessBuilder".equals(call.owner) && "start".equals(call.name)))) {
                    return true;
                }
                if (insn instanceof InvokeDynamicInsnNode indy && indy.bsm != null
                        && !indy.bsm.getOwner().startsWith("java/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<String> references(ClassNode node) {
        return references(node, Set.of());
    }

    private static Set<String> references(ClassNode node, Set<String> skipLaunchesOf) {
        Set<String> refs = new HashSet<>();
        if (node == null) {
            return refs;
        }
        addName(refs, node.superName);
        if (node.interfaces != null) {
            refs.addAll(node.interfaces);
        }
        addName(refs, node.outerClass);
        if (node.innerClasses != null) {
            for (InnerClassNode inner : node.innerClasses) {
                if (node.name.equals(inner.outerName) || node.name.equals(inner.name)) {
                    addName(refs, inner.name);
                }
            }
        }
        if (node.fields != null) {
            for (FieldNode field : node.fields) {
                addDescriptor(refs, field.desc);
            }
        }
        if (node.methods == null) {
            return refs;
        }
        for (MethodNode method : node.methods) {
            addDescriptor(refs, method.desc);
            if (method.instructions == null) {
                continue;
            }
            Set<AbstractInsnNode> skipped = skippedLaunchInsns(method, skipLaunchesOf);
            for (AbstractInsnNode insn : method.instructions) {
                if (!skipped.contains(insn)) {
                    addInsnReferences(refs, insn);
                }
            }
        }
        return refs;
    }

    private static Set<AbstractInsnNode> skippedLaunchInsns(MethodNode method, Set<String> launchedTypes) {
        Set<AbstractInsnNode> skipped = new HashSet<>();
        if (launchedTypes.isEmpty() || !isRegister(method)) {
            return skipped;
        }
        for (AbstractInsnNode insn : method.instructions) {
            String type = launchAt(insn);
            if (type == null || !launchedTypes.contains(type)) {
                continue;
            }
            AbstractInsnNode current = insn;
            for (int i = 0; i < 6 && current != null; i++) {
                skipped.add(current);
                current = nextReal(current);
            }
        }
        return skipped;
    }

    private static void addInsnReferences(Set<String> refs, AbstractInsnNode insn) {
        if (insn instanceof TypeInsnNode typeInsn) {
            addTypeName(refs, typeInsn.desc);
        } else if (insn instanceof MethodInsnNode call) {
            addTypeName(refs, call.owner);
            addDescriptor(refs, call.desc);
        } else if (insn instanceof FieldInsnNode field) {
            addTypeName(refs, field.owner);
            addDescriptor(refs, field.desc);
        } else if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type type) {
            addDescriptor(refs, type.getDescriptor());
        } else if (insn instanceof MultiANewArrayInsnNode array) {
            addDescriptor(refs, array.desc);
        } else if (insn instanceof InvokeDynamicInsnNode indy) {
            addDescriptor(refs, indy.desc);
            addHandle(refs, indy.bsm);
            if (indy.bsmArgs != null) {
                for (Object arg : indy.bsmArgs) {
                    if (arg instanceof Handle handle) {
                        addHandle(refs, handle);
                    } else if (arg instanceof Type type) {
                        addDescriptor(refs, type.getDescriptor());
                    }
                }
            }
        }
    }

    private static void addHandle(Set<String> refs, Handle handle) {
        if (handle != null) {
            addName(refs, handle.getOwner());
            addDescriptor(refs, handle.getDesc());
        }
    }

    private static void addTypeName(Set<String> refs, String name) {
        if (name != null && name.startsWith("[")) {
            addDescriptor(refs, name);
        } else {
            addName(refs, name);
        }
    }

    private static void addName(Set<String> refs, String name) {
        if (name != null) {
            refs.add(name);
        }
    }

    private static void addDescriptor(Set<String> refs, String descriptor) {
        if (descriptor == null) {
            return;
        }
        int start = descriptor.indexOf('L');
        while (start >= 0) {
            int end = descriptor.indexOf(';', start);
            if (end < 0) {
                return;
            }
            refs.add(descriptor.substring(start + 1, end));
            start = descriptor.indexOf('L', end);
        }
    }

    private static String findReferenceToRemoved(List<JarEntryUtils.Entry> kept, Set<String> removed) {
        for (JarEntryUtils.Entry entry : kept) {
            if (internalName(entry) == null) {
                continue;
            }
            try {
                ClassNode node = new ClassNode();
                new ClassReader(entry.data()).accept(node, 0);
                for (String ref : references(node)) {
                    if (removed.contains(ref)) {
                        return entry.name();
                    }
                }
            } catch (Throwable ignored) {
                return entry.name();
            }
        }
        return null;
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode insn) {
        AbstractInsnNode current = insn == null ? null : insn.getNext();
        while (current != null && current.getOpcode() < 0) {
            current = current.getNext();
        }
        return current;
    }

    private static boolean isDup(AbstractInsnNode insn) {
        return insn != null && insn.getOpcode() == Opcodes.DUP;
    }

    private static boolean isThis(AbstractInsnNode insn) {
        return insn instanceof VarInsnNode varInsn
                && varInsn.getOpcode() == Opcodes.ALOAD
                && varInsn.var == 0;
    }

    private static boolean isPlaceholderApi(AbstractInsnNode insn) {
        return insn instanceof MethodInsnNode methodInsn
                && methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL
                && "getPlaceholderAPI".equals(methodInsn.name);
    }

    private static boolean isInitOf(AbstractInsnNode insn, String owner) {
        return insn instanceof MethodInsnNode methodInsn
                && methodInsn.getOpcode() == Opcodes.INVOKESPECIAL
                && "<init>".equals(methodInsn.name)
                && owner.equals(methodInsn.owner);
    }

    private static boolean isPop(AbstractInsnNode insn) {
        return insn != null && insn.getOpcode() == Opcodes.POP;
    }
}
