package bm.b0b0b0.hacks;

import javax.swing.JTextArea;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import bm.b0b0b0.util.asm.JarClassWriter;
import bm.b0b0b0.util.files.CleanPipeline;

public class bStatsJar {

    private final List<String> infectedFiles = new CopyOnWriteArrayList<>();
    private final Path outputPath;
    private final ExecutorService executor;

    private final Conf conf;

    public bStatsJar(Path pluginsPath, Path outputPath, boolean removeMalware, JTextArea consoleArea, Conf conf) {
        this.outputPath = outputPath;
        this.conf = conf;
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            b0b0b0Dick.log(conf.getTranslation("scanningDirectory") + ": " + pluginsPath);
            Files.walk(pluginsPath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> executor.submit(() -> scanFile(path.toFile())));
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            b0b0b0Dick.reportMalware(infectedFiles, consoleArea, conf);
            if (removeMalware) {
                cleanInfectedFiles();
            }
            b0b0b0Dick.log(conf.getTranslation("done"));
        } catch (Exception e) {
            e.printStackTrace();
            b0b0b0Dick.log(conf.getTranslation("error") + ": " + e.getMessage());
        }
    }

    private void cleanInfectedFiles() {
        infectedFiles.forEach(filePath -> {
            try {
                b0b0b0Dick.log(conf.getTranslation("cleaningFile") + ": " + filePath);
            } catch (Exception e) {
                e.printStackTrace();
                b0b0b0Dick.log(conf.getTranslation("errorCleaningFile") + ": " + filePath);
            }
        });
    }

    private void scanFile(File file) {
        if (CleanPipeline.isRejected(file.getName())) {
            return;
        }
        if (file.getName().endsWith(".jar")) {
            try (ZipFile zip = new ZipFile(file)) {
                b0b0b0Dick.log(String.format(conf.getTranslation("scanningFiles"), file.getName()));
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    if (zipEntry.getName().startsWith("org/intellij/lang/annotations") && zipEntry.getName().endsWith(".class")) {
                        try (InputStream inputStream = zip.getInputStream(zipEntry)) {
                            ClassReader classReader = new ClassReader(inputStream);
                            ClassNode classNode = new ClassNode();
                            classReader.accept(classNode, 0);

                            boolean isMalicious = false;
                            String maliciousClassName = null;

                            for (MethodNode method : classNode.methods) {
                                for (AbstractInsnNode insn : method.instructions.toArray()) {
                                    if (insn instanceof LdcInsnNode) {
                                        LdcInsnNode ldc = (LdcInsnNode) insn;
                                        if ("bStats.jar".equals(ldc.cst)) {
                                            isMalicious = true;
                                            maliciousClassName = classNode.name;
                                            b0b0b0Dick.log(String.format(conf.getTranslation("suspiciousClassFound"), classNode.name));
                                            break;
                                        }
                                    }
                                }
                            }

                            if (isMalicious && maliciousClassName != null) {
                                infectedFiles.add(file.getAbsolutePath());
                                runRemover(file, maliciousClassName);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                b0b0b0Dick.log(String.format(conf.getTranslation("errorScanningFiles"), file.getName()));
            }
        }
    }

    private void runRemover(File file, String maliciousClassName) {
        try {
            File outputDir = outputPath.toFile();
            File src = CleanPipeline.sourceFile(file, outputDir);
            File outputFile = CleanPipeline.outputFile(outputDir, file.getName());
            b0b0b0Dick.log(String.format(conf.getTranslation("creatingCleanFile"), outputFile.getAbsolutePath()));

            try (ZipFile zip = new ZipFile(src);
                 ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputFile))) {

                Map<String, byte[]> jarClasses = indexJarClasses(zip);
                byte[] ownerBytes = maliciousClassName.getBytes(StandardCharsets.UTF_8);
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    String entryName = zipEntry.getName();

                    if (entryName.startsWith("org/intellij/lang/annotations/" + maliciousClassName.replace('.', '/') + "$aph")) {
                        b0b0b0Dick.log(String.format(conf.getTranslation("removingFile"), entryName));
                        continue;
                    }

                    if (entryName.startsWith("org/intellij/") || entryName.startsWith("org/jetbrains/")) {
                        b0b0b0Dick.log(String.format(conf.getTranslation("removingFile"), entryName));
                        continue;
                    }

                    if (entryName.endsWith(".class")) {
                        b0b0b0Dick.log(String.format(conf.getTranslation("processingClass"), entryName));
                        processClassEntry(zip, out, zipEntry, maliciousClassName, ownerBytes, jarClasses);
                    } else {
                        try (InputStream entryInputStream = zip.getInputStream(zipEntry)) {
                            out.putNextEntry(new ZipEntry(entryName));
                            b0b0b0Dick.writeZipEntry(out, entryInputStream);
                        } catch (Exception e) {
                            e.printStackTrace();
                            b0b0b0Dick.log(String.format(conf.getTranslation("errorWritingFile"), entryName));
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            if (outputFile.exists() && outputFile.length() > 0) {
                b0b0b0Dick.log(String.format(conf.getTranslation("cleanFileCreatedSuccessfully"), outputFile.getAbsolutePath()));
            } else {
                b0b0b0Dick.log(String.format(conf.getTranslation("failedToCreateCleanFile"), outputFile.getAbsolutePath()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            b0b0b0Dick.log(String.format(conf.getTranslation("errorCleaningFile"), file.getName()));
        }
    }

    private void processClassEntry(ZipFile zip, ZipOutputStream out, ZipEntry zipEntry, String maliciousClassName,
                                   byte[] ownerBytes, Map<String, byte[]> jarClasses) throws IOException {
        byte[] original;
        try (InputStream in = zip.getInputStream(zipEntry)) {
            original = in.readAllBytes();
        }

        out.putNextEntry(new ZipEntry(zipEntry.getName()));

        if (indexOf(original, ownerBytes) == -1) {
            out.write(original);
            return;
        }

        ClassReader cr = new ClassReader(original);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        boolean changed = false;
        for (MethodNode mn : classNode.methods) {
            List<AbstractInsnNode> toRemove = getAbstractInsnNodes(mn, maliciousClassName);
            if (!toRemove.isEmpty()) {
                changed = true;
                toRemove.forEach(mn.instructions::remove);
            }
        }

        if (!changed) {
            out.write(original);
            return;
        }

        out.write(JarClassWriter.writeModified(classNode, jarClasses));
    }

    private static Map<String, byte[]> indexJarClasses(ZipFile zip) throws IOException {
        Map<String, byte[]> map = new HashMap<>();
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                byte[] data = in.readAllBytes();
                map.put(entry.getName(), data);
                map.put(entry.getName().substring(0, entry.getName().length() - ".class".length()), data);
            }
        }
        return map;
    }

    private static int indexOf(byte[] hay, byte[] needle) {
        if (needle.length == 0 || hay.length < needle.length) {
            return -1;
        }
        outer:
        for (int i = 0; i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (hay[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static List<AbstractInsnNode> getAbstractInsnNodes(MethodNode mn, String maliciousClassName) {
        List<AbstractInsnNode> toRemove = new ArrayList<>();
        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (methodInsn.owner.equals(maliciousClassName)
                        && (methodInsn.name.equals("get") || methodInsn.name.equals("getItem"))
                        && methodInsn.desc.equals("(Lorg/bukkit/plugin/java/JavaPlugin;)V")) {
                    AbstractInsnNode prev = insn.getPrevious();
                    if (prev != null && prev.getOpcode() == Opcodes.DUP) {
                        toRemove.add(prev);
                    }
                    toRemove.add(insn);
                }
            }
            insn = insn.getNext();
        }
        return toRemove;
    }
}
