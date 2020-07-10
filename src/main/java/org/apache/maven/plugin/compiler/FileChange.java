package org.apache.maven.plugin.compiler;

import java.io.File;

class FileChange {
    private final File file;

    private final ChangeType changeType;

    public FileChange(File file, ChangeType changeType) {
        this.file = file;
        this.changeType = changeType;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public File getFile() {
        return file;
    }
}
