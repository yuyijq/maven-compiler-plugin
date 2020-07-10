package org.apache.maven.plugin.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

class PureFile implements FileInfo {

    private final File file;

    public PureFile(File file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public void delete() {
        file.delete();
    }

    @Override
    public InputStream open() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public File rawFile() {
        return file;
    }
}
