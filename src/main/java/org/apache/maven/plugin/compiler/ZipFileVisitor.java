package org.apache.maven.plugin.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class ZipFileVisitor implements FileVisitor {

    private final DefaultStreamHasher hasher;

    private final boolean computeHashOnly;

    private final List<ZipEntryInfo> zipEntries;
    private final ClassDependenciesAnalyzer analyzer;
    private final ClassDependentsAccumulator accumulator;

    public ZipFileVisitor(List<ZipEntryInfo> zipEntries) {
        this(null, null, zipEntries, true);
    }

    public ZipFileVisitor(ClassDependenciesAnalyzer analyzer,
                          ClassDependentsAccumulator accumulator,
                          List<ZipEntryInfo> zipEntries) {
        this(analyzer, accumulator, zipEntries, false);
    }

    private ZipFileVisitor(ClassDependenciesAnalyzer analyzer,
                          ClassDependentsAccumulator accumulator,
                          List<ZipEntryInfo> zipEntries,
                          boolean computeHashOnly) {
        this.analyzer = analyzer;
        this.accumulator = accumulator;
        this.hasher = new DefaultStreamHasher();
        this.computeHashOnly = computeHashOnly;
        this.zipEntries = zipEntries;
    }

    @Override
    public void visit(FileInfo file) {
        if (!file.getName().endsWith(".class")) return;

        try {
            HashCode hashCode;
            try (InputStream is = file.open()) {
                hashCode = hasher.hash(is);
            }
            ClassAnalysis classAnalysis = null;
            if (!computeHashOnly) {
                try (InputStream is = file.open()) {
                    classAnalysis = scan(is);
                }
            }
            String className = classAnalysis == null ? null : classAnalysis.getClassName();
            ZipEntryInfo info = new ZipEntryInfo(file.getName(), className, hashCode.hashCode());
            zipEntries.add(info);
        } catch (Exception e) {
            new RuntimeException(e);
        }
    }

    private ClassAnalysis scan(InputStream is) throws IOException {
        ClassAnalysis classAnalysis = analyzer.getClassAnalysis(is);
        accumulator.addClass(classAnalysis);
        return classAnalysis;
    }
}
