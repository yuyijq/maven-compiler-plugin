package org.apache.maven.plugin.compiler;

import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ZipFileScanner {
    public void scan(File zipFile, FileVisitor fileVisitor) {
        if (isNotZip(zipFile)) return;

        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) continue;

                fileVisitor.visit(new ZipEntryFile(zip, zipEntry));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isNotZip(File file) {
        return !file.exists() || file.isDirectory() || !file.getName().endsWith(".jar");
    }
}
