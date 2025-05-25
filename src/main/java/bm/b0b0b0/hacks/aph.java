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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static bm.b0b0b0.util.files.FileUtils.generateUniqueFileName;
import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.gui.Conf;

public class aph {

    private final List<String> infectedFiles = new CopyOnWriteArrayList<>();
    private final Path outputPath;
    private final ExecutorService executor;
    private final Conf conf;

    public aph(Path pluginsPath, Path outputPath, boolean removeMalware, JTextArea consoleArea, Conf conf) {
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
            } catch (Exception e) {
                e.printStackTrace();
                b0b0b0Dick.log(conf.getTranslation("errorCleaningFile") + ": " + filePath);
            }
        });
    }


    private void scanFile(File file) {
        if (file.getName().endsWith(".jar")) {
            try (ZipFile zip = new ZipFile(file)) {
                b0b0b0Dick.log(conf.getTranslation("scanningFile") + ": " + file.getName());
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    if (zipEntry.getName().endsWith(".class")) {
                        try (InputStream inputStream = zip.getInputStream(zipEntry)) {
                            ClassReader classReader = new ClassReader(inputStream);
                            ClassNode classNode = new ClassNode();
                            classReader.accept(classNode, 0);

                            boolean foundAphInjection = false;
                            String className = classNode.name;

                            for (MethodNode method : classNode.methods) {
                                for (AbstractInsnNode insn : method.instructions.toArray()) {
                                    if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                                        MethodInsnNode methodInsn = (MethodInsnNode) insn;
                                        if (methodInsn.owner.endsWith("$aph") && methodInsn.name.equals("inject")) {
                                            foundAphInjection = true;
                                            b0b0b0Dick.log(conf.getTranslation("foundInjectionCall") + ": " + method.name);
                                        }
                                    }
                                }
                            }

                            if (foundAphInjection) {
                                infectedFiles.add(file.getAbsolutePath());
                                processFile(file, conf);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            b0b0b0Dick.log(conf.getTranslation("errorAnalyzingClass") + ": " + zipEntry.getName());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                b0b0b0Dick.log(conf.getTranslation("errorScanningFile") + ": " + file.getName());
            }
        }
    }


    private void processFile(File file, Conf conf) {
        try {
            File outputFile = new File(outputPath.toFile(), generateUniqueFileName(outputPath.toFile(), file.getName()));
            b0b0b0Dick.log(this.conf.getTranslation("creatingCleanFile") + ": " + outputFile.getAbsolutePath());

            try (ZipFile zip = new ZipFile(file);
                 ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputFile))) {

                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    String entryName = zipEntry.getName();

                    if (entryName.endsWith("$aph.class")) {
                        b0b0b0Dick.log(this.conf.getTranslation("removingFile") + ": " + entryName);
                        continue;
                    }

                    if (entryName.endsWith(".class")) {
                        try (InputStream in = zip.getInputStream(zipEntry)) {
                            ClassReader cr = new ClassReader(in);
                            ClassNode classNode = new ClassNode();
                            cr.accept(classNode, 0);

                            if (containsAphInjection(classNode)) {
                                b0b0b0Dick.log(this.conf.getTranslation("removingInjectCall"));
                                removeAphInjection(classNode);
                            }

                            ClassWriter cw = new CustomClassWriter(ClassWriter.COMPUTE_MAXS);
                            classNode.accept(cw);
                            out.putNextEntry(new ZipEntry(zipEntry.getName()));
                            b0b0b0Dick.writeZipEntry(out, new ByteArrayInputStream(cw.toByteArray()));
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try (InputStream entryInputStream = zip.getInputStream(zipEntry)) {
                            out.putNextEntry(new ZipEntry(entryName));
                            b0b0b0Dick.writeZipEntry(out, entryInputStream);
                        } catch (Exception e) {
                            e.printStackTrace();
                            b0b0b0Dick.log(this.conf.getTranslation("errorWritingFile") + ": " + entryName);
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            if (outputFile.exists() && outputFile.length() > 0) {
                b0b0b0Dick.log(this.conf.getTranslation("cleanFileCreated") + ": " + outputFile.getAbsolutePath());
            } else {
                b0b0b0Dick.log(this.conf.getTranslation("cleanFileCreationFailed") + ": " + outputFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            b0b0b0Dick.log(this.conf.getTranslation("errorCleaningFile") + ": " + file.getName());
        }
    }


    private boolean containsAphInjection(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if ("onEnable".equals(method.name)) {
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                        MethodInsnNode methodInsn = (MethodInsnNode) insn;
                        if (methodInsn.owner.endsWith("$aph") && methodInsn.name.equals("inject")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void removeAphInjection(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if ("onEnable".equals(method.name)) {
                List<AbstractInsnNode> toRemove = new ArrayList<>();
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                        MethodInsnNode methodInsn = (MethodInsnNode) insn;
                        if (methodInsn.owner.endsWith("$aph") && methodInsn.name.equals("inject")) {
                            toRemove.add(insn);
                            AbstractInsnNode prev = insn.getPrevious();
                            if (prev != null && prev.getOpcode() == Opcodes.DUP) {
                                toRemove.add(prev);
                            }
                        }
                    }
                }
                toRemove.forEach(method.instructions::remove);
            }
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
}
