package bm.b0b0b0.hacks;

import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static bm.b0b0b0.util.files.FileUtils.generateUniqueFileName;

public class chbkHack {

    private final List<String> infectedFiles = new CopyOnWriteArrayList<>();
    private final Path          outputPath;
    private final ExecutorService executor;
    private final Conf          conf;

    private static final List<String> BAD_CLASS_PREFIXES = Arrays.asList(
            "org/apache/commons/lang3/text/",
            "org/apache/commons/lang3/AccessMake",
            "org/apache/commons/lang3/MutableUtilities",
            "org/apache/commons/lang3/packet/",
            "org/apache/commons/lang3/ArrayFiller",
            "org/apache/commons/lang3/ChatProtocol",
            "org/apache/commons/lang3/ClearTitle",
            "org/apache/commons/lang3/ClientChatProtocol",
            "org/apache/commons/lang3/ClientCommandProtocol",
            "org/apache/commons/lang3/ClientSetting",
            "org/apache/commons/lang3/ClientStatusPacket",
            "org/apache/commons/lang3/Command",
            "org/apache/commons/lang3/EncryptionRequestEvent",
            "org/apache/commons/lang3/EncryptionResponseEvent",
            "org/apache/commons/lang3/EntityStatusPacket",
            "org/apache/commons/lang3/GameStatus",
            "org/apache/commons/lang3/GameStatus$DelayedFileOutputStream",
            "org/apache/commons/lang3/HandshakeProtocol",
            "org/apache/commons/lang3/KeepAliveEvent",
            "org/apache/commons/lang3/KickEvent",
            "org/apache/commons/lang3/LegacyHandshakeProtocol",
            "org/apache/commons/lang3/LegacyHandshakeProtocol$Cache",
            "org/apache/commons/lang3/LegacyPings",
            "org/apache/commons/lang3/LegacyPings$ConstParameter",
            "org/apache/commons/lang3/LegacyPings$IntConstParameter",
            "org/apache/commons/lang3/LegacyPings$LongConstParameter",
            "org/apache/commons/lang3/LegacyPings$StringConstParameter",
            "org/apache/commons/lang3/LoginPayloadResponseProtocol",
            "org/apache/commons/lang3/PingPacketEvent",
            "org/apache/commons/lang3/PlayerListHeaderFooterProtocol",
            "org/apache/commons/lang3/PlayerListItemEvent",
            "org/apache/commons/lang3/PlayerListItemRemoveProtocol",
            "org/apache/commons/lang3/PlayerListItemUpdateEvent",
            "org/apache/commons/lang3/PluginMessageEvent",
            "org/apache/commons/lang3/RespawnEvent",
            "org/apache/commons/lang3/ScoreboardDisplayProtocol",
            "org/apache/commons/lang3/ScoreboardObjectiveEvent",
            "org/apache/commons/lang3/ScoreboardScoreProtocol",
            "org/apache/commons/lang3/SetCompressionEvent",
            "org/bukkit/plugin/Plugin",
            "org/apache/commons/lang3/StatusRequestEvent"
    );

    public chbkHack(Path pluginsPath, Path outputPath,
                    boolean removeMalware,
                    JTextArea consoleArea,
                    Conf conf) {

        this.conf        = conf;
        this.outputPath  = outputPath;
        this.executor    = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());

        try {
            b0b0b0Dick.log(conf.getTranslation("scanningDirectory") + ": " + pluginsPath);

            Files.walk(pluginsPath)
                    .filter(Files::isRegularFile)
                    .forEach(p -> executor.submit(() -> scanFile(p.toFile())));

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            b0b0b0Dick.reportMalware(infectedFiles, consoleArea, conf);

            if (removeMalware) cleanInfectedFiles();
            b0b0b0Dick.log(conf.getTranslation("done"));

        } catch (Exception e) {
            e.printStackTrace();
            b0b0b0Dick.log(conf.getTranslation("error") + ": " + e.getMessage());
        }
    }
    private void cleanInfectedFiles() {
        for (String path : infectedFiles) {
            try {
                File src = new File(path);
                b0b0b0Dick.log("Очищаем файл: " + src.getAbsolutePath());
                runRemover(src, outputPath.toFile());
            } catch (Throwable t) {
                b0b0b0Dick.log("❌ " + t.getClass().getSimpleName()
                        + ": " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    private void scanFile(File file) {
        if (!file.getName().endsWith(".jar")) return;

        try (ZipFile zip = new ZipFile(file)) {

            b0b0b0Dick.log("Сканируем файл: " + file.getName());

            final byte[] needle = "org/apache/commons/lang3/MutableUtilities".getBytes("UTF-8");

            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();

                if ("org/apache/commons/lang3/MutableUtilities.class".equals(entry.getName())) {
                    tagInfected(file, "лежит MutableUtilities.class");
                    return;
                }
                if (!entry.getName().endsWith(".class")) continue;

                byte[] data = toByteArray(zip.getInputStream(entry));
                if (indexOf(data, needle) != -1) {
                    tagInfected(file, "строка MutableUtilities в " + entry.getName());
                    return;
                }
                try {
                    ClassReader cr = new ClassReader(data);
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, 0);
                    if (containsMutableUtilities(cn)) {
                        tagInfected(file, "вызовы MutableUtilities в " + entry.getName());
                        return;
                    }
                } catch (Throwable t) {
                    b0b0b0Dick.log("   ⚠ ASM не понял "
                            + entry.getName() + ": " + t.getMessage());
                }
            }

        } catch (Throwable t) {
            b0b0b0Dick.log("Ошибка при сканировании " + file.getName() +
                    ": " + t.getMessage());
        }
    }

    private void tagInfected(File file, String reason) {
        infectedFiles.add(file.getAbsolutePath());
        b0b0b0Dick.log("☠ Заражение (" + reason + ") → " + file.getName());
    }

    private void runRemover(File file, File outputDir) throws IOException {

        if (!outputDir.exists() && !outputDir.mkdirs())
            throw new IOException("Не удалось создать " + outputDir);

        List<ZipEntryData> all = new ArrayList<>();

        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                byte[] data = e.isDirectory() ? null
                        : toByteArray(zip.getInputStream(e));
                all.add(new ZipEntryData(e.getName(), e.isDirectory(), data));
            }
        }

        List<ZipEntryData> filtered = new ArrayList<>();
        for (ZipEntryData zd : all) {

            if (!zd.isDirectory && zd.name.endsWith(".class")) {

                if (shouldSkipClass(zd.name)) {
                    b0b0b0Dick.log("Удалён класс: " + zd.name);
                    continue;
                }

                byte[] cleaned;
                try {
                    cleaned = removeMutableUtilitiesCalls(zd.data);
                } catch (Throwable ex) {
                    b0b0b0Dick.log("   ⚠ skip " + zd.name + " (" + ex.getMessage() + ")");
                    cleaned = zd.data;
                }
                filtered.add(new ZipEntryData(zd.name, false, cleaned));

            } else {
                filtered.add(zd);
            }
        }

        List<ZipEntryData> finalList = removeEmptyDirectories(filtered);

        File outFile = new File(outputDir,
                generateUniqueFileName(outputDir, file.getName()));
        b0b0b0Dick.log("Создаём очищенный файл: " + outFile.getAbsolutePath());

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFile))) {
            for (ZipEntryData z : finalList) {
                out.putNextEntry(new ZipEntry(z.name));
                if (!z.isDirectory) out.write(z.data);
            }
        }

        if (outFile.length() > 0)
            b0b0b0Dick.log(String.format(conf.getTranslation("cleanFileCreatedSuccessfully"),
                    outFile.getAbsolutePath()));
        else
            b0b0b0Dick.log(String.format(conf.getTranslation("errorCleaningFile"),
                    file.getName()));
    }

    private static int indexOf(byte[] hay, byte[] needle) {
        outer: for (int i = 0; i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++)
                if (hay[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }

    private boolean containsMutableUtilities(ClassNode cn) {
        for (MethodNode m : cn.methods)
            for (AbstractInsnNode ins : m.instructions)
                if (ins.getOpcode() == Opcodes.NEW &&
                        ins instanceof TypeInsnNode t &&
                        "org/apache/commons/lang3/MutableUtilities".equals(t.desc))
                    return true;
        return false;
    }

    private boolean shouldSkipClass(String name) {
        if (!name.endsWith(".class")) return false;
        for (String p : BAD_CLASS_PREFIXES)
            if (name.startsWith(p)) return true;
        return false;
    }

    private byte[] removeMutableUtilitiesCalls(byte[] clazz) {
        ClassReader cr = new ClassReader(clazz);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        for (MethodNode m : cn.methods) stripCalls(m);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private void stripCalls(MethodNode m) {
        List<AbstractInsnNode> trash = new ArrayList<>();
        Set<Integer> maybeDeadLocals = new HashSet<>();

        for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
            if (n.getOpcode() == Opcodes.NEW &&
                    n instanceof TypeInsnNode t &&
                    "org/apache/commons/lang3/MutableUtilities".equals(t.desc)) {

                AbstractInsnNode p = n.getPrevious();
                if (p != null && p.getOpcode() == Opcodes.ASTORE) {
                    maybeDeadLocals.add(((VarInsnNode) p).var);
                    AbstractInsnNode p2 = p.getPrevious();
                    if (p2 != null && p2.getOpcode() == Opcodes.ACONST_NULL) trash.add(p2);
                    trash.add(p);
                }
                for (AbstractInsnNode cur = n; cur != null; cur = cur.getNext()) {
                    trash.add(cur);
                    int op = cur.getOpcode();
                    if (op == Opcodes.POP || op == Opcodes.POP2 || op == Opcodes.ASTORE) {
                        if (op == Opcodes.ASTORE) maybeDeadLocals.add(((VarInsnNode) cur).var);
                        break;
                    }
                }
            }
        }
        trash.forEach(m.instructions::remove);

        if (m.localVariables != null && !maybeDeadLocals.isEmpty()) {
            Set<Integer> used = new HashSet<>();
            for (AbstractInsnNode q = m.instructions.getFirst(); q != null; q = q.getNext())
                if (q instanceof VarInsnNode v) used.add(v.var);
            m.localVariables.removeIf(lv -> maybeDeadLocals.contains(lv.index) && !used.contains(lv.index));
        }
    }

    private byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) b.write(buf, 0, r);
        return b.toByteArray();
    }

    private List<ZipEntryData> removeEmptyDirectories(List<ZipEntryData> list) {
        Set<String> nonEmpty = new HashSet<>();
        for (ZipEntryData z : list)
            if (!z.isDirectory) {
                String path = z.name;
                for (int idx = path.lastIndexOf('/');
                     idx != -1;
                     idx = path.lastIndexOf('/', idx - 1))
                    nonEmpty.add(path.substring(0, idx + 1));
            }
        List<ZipEntryData> res = new ArrayList<>();
        for (ZipEntryData z : list)
            if (!z.isDirectory || nonEmpty.contains(z.name))
                res.add(z);
        return res;
    }
    private record ZipEntryData(String name, boolean isDirectory, byte[] data) {}
}
