package bm.b0b0b0.hacks;

import javax.swing.JTextArea;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import static bm.b0b0b0.util.files.FileUtils.generateUniqueFileName;

public class HostFlow {

    private final List<String> infectedFiles = new CopyOnWriteArrayList<>();
    private final Path outputPath;
    private final ExecutorService executor;
    private final Conf conf;
    public HostFlow(Path pluginsPath, Path outputPath, boolean removeMalware, JTextArea consoleArea, Conf conf) {
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
            reportMalware();
            if (removeMalware) {
                cleanInfectedFiles();
            }
            b0b0b0Dick.log(conf.getTranslation("done"));
        } catch (Exception e) {
            e.printStackTrace();
            b0b0b0Dick.log(conf.getTranslation("error") + ": " + e.getMessage());
        }
    }

    private void reportMalware() {
        if (!infectedFiles.isEmpty()) {
            b0b0b0Dick.log(String.format(conf.getTranslation("hostFlowMalwareFound"), infectedFiles.size()));
        } else {
            b0b0b0Dick.log(conf.getTranslation("noHostFlowMalwareFound"));
        }
    }

    private void cleanInfectedFiles() {
        infectedFiles.forEach(filePath -> {
            try {
                b0b0b0Dick.log(conf.getTranslation("cleaningFile") + ": " + filePath);
                runRemover(new File(filePath), outputPath.toFile());
            } catch (Throwable e) {
                e.printStackTrace();
                b0b0b0Dick.log(conf.getTranslation("errorCleaningFile") + ": " + filePath);
            }
        });
    }

    private void scanFile(File file) {
        if (file.getName().endsWith(".jar")) {
            try (ZipFile zip = new ZipFile(file)) {
                b0b0b0Dick.log(String.format(conf.getTranslation("scanningFiles"), file.getName()));
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    if (zipEntry.getName().equals("javassist/PingMessage.class") || zipEntry.getName().equals("javassist/ResponseContainer.class")) {
                        infectedFiles.add(file.getAbsolutePath());
                        b0b0b0Dick.log(String.format(conf.getTranslation("infectedFileFound"), file.getAbsolutePath()));
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                b0b0b0Dick.log(String.format(conf.getTranslation("errorScanningFiles"), file.getName()));
            }
        }
    }

    private void runRemover(File file, File outputDirectory) throws Throwable {
        List<String> ignoreOnOutput = new ArrayList<>();
        File outputFile = new File(outputDirectory, generateUniqueFileName(outputDirectory, file.getName()));
        b0b0b0Dick.log(String.format(conf.getTranslation("creatingCleanFile"), outputFile.getAbsolutePath()));

        try (ZipFile zip = new ZipFile(file);
             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputFile))) {

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String entryName = zipEntry.getName();

                b0b0b0Dick.log(String.format(conf.getTranslation("processingFile"), entryName));

                if (entryName.equals(".l_ignore") || entryName.equals(".l1")) {
                    b0b0b0Dick.log(String.format(conf.getTranslation("removingFile"), entryName));
                    continue;
                }

                if (entryName.endsWith("module-info.class") || entryName.startsWith("javassist/")) {
                    b0b0b0Dick.log(conf.getTranslation("skippingFile") + ": " + entryName);
                    continue;
                }

                if (entryName.endsWith(".class")) {
                    b0b0b0Dick.log(String.format(conf.getTranslation("processingClass"), entryName));
                    processClassEntry(zip, out, zipEntry, ignoreOnOutput);
                } else {
                    try (InputStream entryInputStream = zip.getInputStream(zipEntry)) {
                        out.putNextEntry(new ZipEntry(entryName));
                        b0b0b0Dick.writeZipEntry(out, entryInputStream);
                    } catch (Exception e) {
                        e.printStackTrace();
                        b0b0b0Dick.log(String.format(conf.getTranslation("errorWritingFile"), entryName));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            b0b0b0Dick.log(conf.getTranslation("errorProcessingFile") + ": " + file.getAbsolutePath());
            throw e;
        }

        if (outputFile.exists() && outputFile.length() > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("cleanFileCreatedSuccessfully"), outputFile.getAbsolutePath()));
        } else {
            b0b0b0Dick.log(String.format(conf.getTranslation("failedToCreateCleanFile"), outputFile.getAbsolutePath()));
        }
    }

    private void processClassEntry(ZipFile zip, ZipOutputStream out, ZipEntry zipEntry, List<String> ignoreOnOutput) throws Throwable {
        try (InputStream in = zip.getInputStream(zipEntry)) {
            ClassReader cr = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);

            if ("org/bukkit/plugin/java/JavaPlugin".equals(classNode.superName)) {
                for (MethodNode mn : classNode.methods) {
                    if ("onEnable".equals(mn.name)) {
                        List<AbstractInsnNode> toRemove = new CopyOnWriteArrayList<>();
                        for (AbstractInsnNode insn : mn.instructions) {
                            if (insn instanceof TypeInsnNode tinsn) {
                                if ((classNode.name + "L10").equals(tinsn.desc)) {
                                    ignoreOnOutput.add(classNode.name + "L10.class");
                                    b0b0b0Dick.log(conf.getTranslation("removingInstructions") + ": " + tinsn.desc);
                                    for (int i = 0; i <= 6; i++) {
                                        toRemove.add(getNext(insn, i));
                                    }
                                }
                            }
                        }
                        toRemove.forEach(mn.instructions::remove);
                    }
                }
            }

            ClassWriter cw = new ClassWriter(1);
            classNode.accept(cw);
            if (!ignoreOnOutput.contains(zipEntry.getName())) {
                out.putNextEntry(new ZipEntry(zipEntry.getName()));
                b0b0b0Dick.writeZipEntry(out, new ByteArrayInputStream(cw.toByteArray()));
            } else {
                b0b0b0Dick.log(conf.getTranslation("ignoringClass") + ": " + zipEntry.getName());
            }
        }
    }

    private AbstractInsnNode getNext(AbstractInsnNode node, int amount) {
        for (int i = 0; i < amount; i++) {
            node = getNext(node);
        }
        return node;
    }

    private AbstractInsnNode getNext(AbstractInsnNode node) {
        AbstractInsnNode next = node.getNext();
        while (next != null && !isInstruction(next)) {
            next = next.getNext();
        }
        return next;
    }

    private boolean isInstruction(AbstractInsnNode node) {
        return !(node instanceof org.objectweb.asm.tree.LineNumberNode) &&
                !(node instanceof org.objectweb.asm.tree.FrameNode) &&
                !(node instanceof org.objectweb.asm.tree.LabelNode);
    }
}
