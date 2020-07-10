package org.apache.maven.plugin.compiler;

import java.io.File;

class DirectoryScanner {
    public void scan(File start, FileVisitor visitor) {
        if (start == null) return;
        if (start.isDirectory()) {
            File[] files = start.listFiles();
            for (File child : files) {
                scan(child, visitor);
            }
        } else if (start.isFile()) {
            visitor.visit(new PureFile(start));
        }
    }
}
