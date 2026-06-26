import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class TestRuBstats {
    public static void main(String[] args) throws Exception {
        String in = args[0], out = args[1];
        List<byte[]> entries = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Boolean> dirs = new ArrayList<>();
        try (ZipFile zip = new ZipFile(in)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                names.add(e.getName());
                dirs.add(e.isDirectory());
                entries.add(e.isDirectory() ? null : zip.getInputStream(e).readAllBytes());
            }
        }
        int skipped = 0, fail = 0, rewritten = 0;
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))) {
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                if (name.startsWith("ru/bstats/")) { skipped++; continue; }
                if (!dirs.get(i) && name.endsWith(".class")) {
                    byte[] data = entries.get(i);
                    try {
                        ClassReader cr = new ClassReader(data);
                        ClassNode cn = new ClassNode();
                        cr.accept(cn, 0);
                        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        cn.accept(cw);
                        data = cw.toByteArray();
                        rewritten++;
                    } catch (Throwable t) {
                        System.out.println("FAIL " + name + ": " + t.getClass().getSimpleName() + " " + t.getMessage());
                        fail++;
                    }
                    zos.putNextEntry(new ZipEntry(name));
                    zos.write(data);
                    zos.closeEntry();
                } else if (!dirs.get(i)) {
                    zos.putNextEntry(new ZipEntry(name));
                    zos.write(entries.get(i));
                    zos.closeEntry();
                }
            }
        }
        System.out.println("skipped=" + skipped + " rewritten=" + rewritten + " fail=" + fail);
    }
}