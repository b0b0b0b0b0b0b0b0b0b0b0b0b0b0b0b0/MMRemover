package bm.b0b0b0.util.inspector.scan;

import bm.b0b0b0.hacks.FakeBstatsMalwareDetector;
import bm.b0b0b0.util.inspector.model.InspectionReport;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class JarScanner {

    private static final String[] DESCRIPTOR_ENTRIES = {
            "plugin.yml",
            "bungee.yml",
            "paper-plugin.yml",
            "velocity-plugin.json",
    };

    private JarScanner() {
    }

    public static InspectionReport scan(File jarFile) {
        InspectionReport report = new InspectionReport();
        report.jarFile = jarFile;
        report.jarName = jarFile == null ? "(null)" : jarFile.getName();
        report.jarSize = jarFile == null ? 0 : jarFile.length();

        if (jarFile == null || !jarFile.exists() || !jarFile.isFile()) {
            report.errors.add("file does not exist or is not a regular file");
            return report;
        }

        List<String> entryNames = new ArrayList<>();
        List<byte[]> classFiles = new ArrayList<>();

        try (ZipFile zip = new ZipFile(jarFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                entryNames.add(name);
                report.totalEntries++;

                byte[] data;
                try (var input = zip.getInputStream(entry)) {
                    data = input.readAllBytes();
                } catch (Throwable throwable) {
                    continue;
                }

                if (name.endsWith(".class")) {
                    report.classCount++;
                    trackObfuscationHeuristic(name, report);
                    FindingCollector.collectRuntimeMarkers(name, data, report);
                    for (String string : ClassStringExtractor.extract(data)) {
                        FindingCollector.collectText(name, string, report);
                    }
                    classFiles.add(data);
                } else if (isDescriptor(name)) {
                    report.pluginDescriptorName = name;
                    report.pluginDescriptor = new String(data, StandardCharsets.UTF_8);
                    FindingCollector.collectText(name, report.pluginDescriptor, report);
                } else if (isTextResource(name)) {
                    FindingCollector.collectText(name, new String(data, StandardCharsets.UTF_8), report);
                }
            }
        } catch (IOException ioException) {
            report.errors.add("I/O error: " + ioException.getMessage());
            return report;
        }

        applyMalwareAnalysis(entryNames, classFiles, report);
        return report;
    }

    private static void applyMalwareAnalysis(List<String> entryNames,
                                             List<byte[]> classFiles,
                                             InspectionReport report) {
        FakeBstatsMalwareDetector.Analysis analysis =
                FakeBstatsMalwareDetector.analyzeJar(entryNames, classFiles);
        if (!analysis.infected()) {
            return;
        }
        report.malwareDetected = true;
        report.malwareReason = analysis.reason();
        report.malwareRoots.addAll(analysis.malwareRoots());
    }

    private static boolean isDescriptor(String name) {
        for (String candidate : DESCRIPTOR_ENTRIES) {
            if (candidate.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTextResource(String name) {
        return name.endsWith(".yml")
                || name.endsWith(".yaml")
                || name.endsWith(".json")
                || name.endsWith(".properties")
                || name.endsWith(".txt")
                || name.endsWith(".cfg")
                || name.endsWith(".conf");
    }

    private static void trackObfuscationHeuristic(String entryName, InspectionReport report) {
        int slash = entryName.lastIndexOf('/');
        String simple = (slash >= 0 ? entryName.substring(slash + 1) : entryName).replace(".class", "");
        int dollar = simple.indexOf('$');
        String base = dollar >= 0 ? simple.substring(0, dollar) : simple;
        if (!base.isEmpty()
                && base.length() <= 2
                && Character.isLetter(base.charAt(0))
                && base.chars().allMatch(Character::isLetterOrDigit)) {
            report.shortNameClasses++;
        }
    }
}
