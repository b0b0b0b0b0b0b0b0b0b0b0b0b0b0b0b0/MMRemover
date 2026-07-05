package bm.b0b0b0.util.dev;

import bm.b0b0b0.hacks.chbkHack;
import bm.b0b0b0.util.gui.Conf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.io.File;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class VerifyOne {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: VerifyOne <jar> <internalClass> [cleanOutDir]");
            System.exit(2);
        }
        File jar = new File(args[0]);
        String internal = args[1];
        String binary = internal.replace('/', '.');

        System.out.println("JAR: " + jar.getAbsolutePath() + " (" + jar.length() + " bytes)");
        System.out.println("SHA256: " + sha256(jar));

        try (ZipFile zip = new ZipFile(jar)) {
            boolean hasPayload = zip.getEntry("org/apache/commons/lang3/MutableUtilities.class") != null;
            System.out.println("MutableUtilities payload: " + hasPayload);
        }

        inspectMethod(jar, internal, methodFor(jar.getName(), internal));

        try {
            JvmVerify.verifyJarClass(jar, binary);
            System.out.println("JvmVerify: OK " + binary);
        } catch (VerifyError ve) {
            System.err.println("JvmVerify: VerifyError " + ve.getMessage());
        }

        if (args.length >= 3) {
            File outDir = new File(args[2]);
            outDir.mkdirs();
            chbkHack.cleanJar(jar, outDir, new Conf());
            File cleaned = new File(outDir, jar.getName());
            System.out.println("Cleaned: " + cleaned.getAbsolutePath() + " (" + cleaned.length() + " bytes)");
            System.out.println("Cleaned SHA256: " + sha256(cleaned));
            inspectMethod(cleaned, internal, methodFor(jar.getName(), internal));
            try {
                JvmVerify.verifyJarClass(cleaned, binary);
                System.out.println("JvmVerify cleaned: OK");
            } catch (VerifyError ve) {
                System.err.println("JvmVerify cleaned: VerifyError " + ve.getMessage());
            }
        }
    }

    private static String methodFor(String jarName, String internal) {
        if (internal.endsWith("WorldGuardPlugin")) {
            return "onCommand";
        }
        if (internal.endsWith("MultiverseCore")) {
            return "loadConfigs";
        }
        if (internal.endsWith("Essentials")) {
            return "onCommandEssentials";
        }
        return "onEnable";
    }

    private static void inspectMethod(File jar, String internal, String methodName) throws Exception {
        String entry = internal + ".class";
        byte[] bytes;
        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry e = zip.getEntry(entry);
            if (e == null) {
                System.out.println("Missing " + entry);
                return;
            }
            bytes = zip.getInputStream(e).readAllBytes();
        }
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG);
        for (MethodNode m : cn.methods) {
            if (!m.name.equals(methodName)) {
                continue;
            }
            int nullCatch = 0;
            int throwableCatch = 0;
            int typed = 0;
            if (m.tryCatchBlocks != null) {
                for (TryCatchBlockNode b : m.tryCatchBlocks) {
                    if (b.type == null) {
                        nullCatch++;
                    } else if ("java/lang/Throwable".equals(b.type)) {
                        throwableCatch++;
                    } else {
                        typed++;
                    }
                }
            }
            System.out.println(methodName + " handlers: null=" + nullCatch
                    + " Throwable=" + throwableCatch + " typed=" + typed
                    + " MutableUtilitiesRefs=" + chbkHack.classReferencesMutableUtilities(bytes));
            return;
        }
        System.out.println("Method not found: " + methodName);
    }

    private static String sha256(File file) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(file.toPath()));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
