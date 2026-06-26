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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import java.nio.charset.StandardCharsets;

import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static bm.b0b0b0.util.files.FileUtils.generateUniqueFileName;

public class Artemka {

    private final List<String> infectedFiles = new CopyOnWriteArrayList<>();
    private final Path outputPath;
    private final ExecutorService executor;
    private final Conf conf;

    public Artemka(Path pluginsPath, Path outputPath, boolean removeMalware, JTextArea consoleArea, Conf conf) {
        this.conf = conf;
        this.outputPath = outputPath;
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
                File file = new File(filePath);
                runRemover(file, outputPath.toFile());
            } catch (Throwable e) {
                b0b0b0Dick.log(conf.getTranslation("errorCleaningFile") + ": " + filePath);
                e.printStackTrace();
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

                    if (zipEntry.getName().equals("org/intellij/lang/annotations/Preconditions.class")) {
                        if (!infectedFiles.contains(file.getAbsolutePath())) {
                            infectedFiles.add(file.getAbsolutePath());
                            b0b0b0Dick.log(String.format(conf.getTranslation("infectedFileFoundArtemka"), file.getAbsolutePath()));
                        }
                        break;
                    }

                    if (zipEntry.getName().endsWith(".class")) {
                        try (InputStream inputStream = zip.getInputStream(zipEntry)) {
                            byte[] data = inputStream.readAllBytes();
                            if (indexOf(data, "org/intellij/lang/annotations/Preconditions".getBytes(StandardCharsets.UTF_8)) == -1) {
                                continue;
                            }
                            ClassReader classReader = new ClassReader(data);
                            ClassNode classNode = new ClassNode();
                            classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                            for (MethodNode method : classNode.methods) {
                                for (AbstractInsnNode insn : method.instructions.toArray()) {
                                    if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                                        MethodInsnNode methodInsn = (MethodInsnNode) insn;
                                        if (methodInsn.owner.equals("org/intellij/lang/annotations/Preconditions")
                                                && methodInsn.name.equals("notNull")
                                                && methodInsn.desc.equals("(Ljava/lang/Object;)V")) {
                                            if (!infectedFiles.contains(file.getAbsolutePath())) {
                                                infectedFiles.add(file.getAbsolutePath());
                                                b0b0b0Dick.log(String.format(conf.getTranslation("infectedFileFound"), file.getAbsolutePath()));
                                            }
                                            break;
                                        }
                                    }
                                }
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

    private void runRemover(File file, File outputDirectory) throws Throwable {
        File outputFile = new File(outputDirectory, generateUniqueFileName(outputDirectory, file.getName()));
        b0b0b0Dick.log(String.format(conf.getTranslation("creatingCleanFile"), outputFile.getAbsolutePath()));

        try (ZipFile zip = new ZipFile(file);
             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputFile))) {

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String entryName = zipEntry.getName();

                if (entryName.startsWith("org/intellij/lang/annotations/Preconditions")) {
                    b0b0b0Dick.log(String.format(conf.getTranslation("removingFile"), entryName));
                    continue;
                }

                if (entryName.startsWith("org/intellij/") || entryName.startsWith("org/jetbrains/")) {
                    b0b0b0Dick.log(String.format(conf.getTranslation("removingFile"), entryName));
                    continue;
                }

                if (entryName.endsWith(".class")) {
                    b0b0b0Dick.log(String.format(conf.getTranslation("processingClass"), entryName));
                    processClassEntry(zip, out, zipEntry);
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
        }
        if (outputFile.exists() && outputFile.length() > 0) {
            b0b0b0Dick.log(String.format(conf.getTranslation("cleanFileCreatedSuccessfully"), outputFile.getAbsolutePath()));
        } else {
            b0b0b0Dick.log(String.format(conf.getTranslation("errorCleaningFile"), file.getName()));
        }
    }

    private void processClassEntry(ZipFile zip, ZipOutputStream out, ZipEntry zipEntry) throws Throwable {
        try (InputStream in = zip.getInputStream(zipEntry)) {
            ClassReader cr = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);

            for (MethodNode mn : classNode.methods) {
                List<AbstractInsnNode> toRemove = getAbstractInsnNodes(mn);
                toRemove.forEach(mn.instructions::remove);
            }

            ClassWriter cw = new CustomClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(cw);
            out.putNextEntry(new ZipEntry(zipEntry.getName()));
            b0b0b0Dick.writeZipEntry(out, new ByteArrayInputStream(cw.toByteArray()));
        }
    }

    private static class CustomClassWriter extends ClassWriter {
        public CustomClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return "java/lang/Object";
        }
    }

    private static List<AbstractInsnNode> getAbstractInsnNodes(MethodNode mn) {
        List<AbstractInsnNode> toRemove = new ArrayList<>();
        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (methodInsn.owner.equals("org/intellij/lang/annotations/Preconditions")
                        && methodInsn.name.equals("notNull")
                        && methodInsn.desc.equals("(Ljava/lang/Object;)V")) {
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

}
