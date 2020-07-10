package org.apache.maven.plugin.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ZipEntryFile implements FileInfo {
    private final ZipFile zipFile;
    private final ZipEntry zipEntry;

    public ZipEntryFile(ZipFile zipFile, ZipEntry zipEntry) {
        this.zipFile = zipFile;
        this.zipEntry = zipEntry;
    }

    @Override
    public String getName() {
        return zipEntry.getName();
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("zip entry dont support this method");
    }

    @Override
    public InputStream open() throws IOException {
        return zipFile.getInputStream(zipEntry);
    }

    @Override
    public long lastModified() {
        return -1;
    }

    @Override
    public File rawFile() {
        return null;
    }
}
