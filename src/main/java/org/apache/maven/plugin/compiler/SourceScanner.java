package org.apache.maven.plugin.compiler;

import java.io.File;
import java.util.List;

class SourceScanner {

    private final SourceMapping sourceMapping;

    public SourceScanner(SourceMapping sourceMapping) {
        this.sourceMapping = sourceMapping;
    }

    public void scan(List<String> sourceRoot) {
        for (String root : sourceRoot) {
            File start = new File(root);
            new DirectoryScanner().scan(start, file -> {
                if (!file.getName().endsWith(".java")) return;
                String path = Util.relative(file.rawFile(), root);
                sourceMapping.add(root, path);
            });
        }
    }
}
