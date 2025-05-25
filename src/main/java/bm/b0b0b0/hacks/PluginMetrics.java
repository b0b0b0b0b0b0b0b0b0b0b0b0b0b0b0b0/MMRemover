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
import bm.b0b0b0.hacks.RussianHuy.b0b0b0Dick;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static bm.b0b0b0.util.files.FileUtils.generateUniqueFileName;

public class PluginMetrics {

    private final List<String> infectedFiles = new CopyOnWriteArrayList<>();
    private final Path outputPath;
    private final ExecutorService executor;
    private final Conf conf;
    public PluginMetrics(Path pluginsPath, Path outputPath, boolean removeMalware, JTextArea consoleArea, Conf conf) {
        this.conf = conf;
        this.outputPath = outputPath;
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            b0b0b0Dick.log(conf.getTranslation("scanningDirectory") + pluginsPath);
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

                            boolean foundPluginMetrics = false;
                            List<String> methodNames = new ArrayList<>();
                            List<String> resourceNames = new ArrayList<>();

                            for (MethodNode method : classNode.methods) {
                                for (AbstractInsnNode insn : method.instructions.toArray()) {
                                    if (insn instanceof LdcInsnNode) {
                                        LdcInsnNode ldc = (LdcInsnNode) insn;
                                        if ("plugins/PluginMetrics.jar".equals(ldc.cst)) {
                                            foundPluginMetrics = true;
                                            methodNames.add(method.name);
                                            b0b0b0Dick.log(String.format(conf.getTranslation("foundPluginMetrics"), method.name));
                                        }
                                        if (ldc.cst instanceof String && ldc.cst.toString().endsWith(".yml")) {
                                            resourceNames.add(ldc.cst.toString());
                                            b0b0b0Dick.log(String.format(conf.getTranslation("foundResourceFile"), ldc.cst.toString()));
                                        }
                                    }
                                }
                            }

                            if (foundPluginMetrics) {
                                infectedFiles.add(file.getAbsolutePath());
                                runRemover(file, methodNames, resourceNames);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            b0b0b0Dick.log(String.format(conf.getTranslation("errorAnalyzingClasss"), zipEntry.getName()));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                b0b0b0Dick.log(String.format(conf.getTranslation("errorScanningFiles"), file.getName()));
            }
        }
    }
    private void runRemover(File file, List<String> methodNames, List<String> resourceNames) {
        try {
            File outputFile = new File(outputPath.toFile(), generateUniqueFileName(outputPath.toFile(), file.getName()));
            b0b0b0Dick.log(String.format(conf.getTranslation("creatingCleanFile"), outputFile.getAbsolutePath()));

            try (ZipFile zip = new ZipFile(file);
                 ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputFile))) {

                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    String entryName = zipEntry.getName();

                    if (resourceNames.contains(entryName)) {
                        b0b0b0Dick.log(String.format(conf.getTranslation("removingResourceFile"), entryName));
                        continue;
                    }

                    if (entryName.endsWith(".class")) {
                        try (InputStream in = zip.getInputStream(zipEntry)) {
                            ClassReader cr = new ClassReader(in);
                            ClassNode classNode = new ClassNode();
                            cr.accept(classNode, 0);

                            boolean classModified = false;
                            if (containsMethodCalls(classNode, methodNames)) {
                                b0b0b0Dick.log(String.format(conf.getTranslation("removingMethodCalls"), methodNames));
                                removeMethodCalls(classNode, methodNames);
                                classModified = true;
                            }

                            if (removeMaliciousMethods(classNode, methodNames)) {
                                b0b0b0Dick.log(String.format(conf.getTranslation("removingMethods"), methodNames));
                                classModified = true;
                            }

                            if (classModified) {
                                ClassWriter cw = new CustomClassWriter(ClassWriter.COMPUTE_MAXS);
                                classNode.accept(cw);
                                out.putNextEntry(new ZipEntry(zipEntry.getName()));
                                b0b0b0Dick.writeZipEntry(out, new ByteArrayInputStream(cw.toByteArray()));
                            } else {
                                try (InputStream entryInputStream = zip.getInputStream(zipEntry)) {
                                    out.putNextEntry(new ZipEntry(entryName));
                                    b0b0b0Dick.writeZipEntry(out, entryInputStream);
                                }
                            }
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
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


    private boolean containsMethodCalls(ClassNode classNode, List<String> methodNames) {
        for (MethodNode method : classNode.methods) {
            if ("onEnable".equals(method.name)) {
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL || insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                        MethodInsnNode methodInsn = (MethodInsnNode) insn;
                        if (methodNames.contains(methodInsn.name)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void removeMethodCalls(ClassNode classNode, List<String> methodNames) {
        for (MethodNode method : classNode.methods) {
            if ("onEnable".equals(method.name)) {
                List<AbstractInsnNode> toRemove = new ArrayList<>();
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL || insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                        MethodInsnNode methodInsn = (MethodInsnNode) insn;
                        if (methodNames.contains(methodInsn.name)) {
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
    private boolean removeMaliciousMethods(ClassNode classNode, List<String> methodNames) {
        List<MethodNode> methodsToRemove = new ArrayList<>();
        for (MethodNode method : classNode.methods) {
            if (methodNames.contains(method.name)) {
                methodsToRemove.add(method);
            }
        }
        return classNode.methods.removeAll(methodsToRemove);
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
