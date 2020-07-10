package org.apache.maven.plugin.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

class ClassAnalysisFileVisitor implements FileVisitor {
    private final String outputDir;
    private final ClassDependenciesAnalyzer analyzer;
    private final ClassDependentsAccumulator accumulator;
    private final SourceMapping sourceMapping;

    public ClassAnalysisFileVisitor(File outputDir, ClassDependenciesAnalyzer analyzer, ClassDependentsAccumulator accumulator, SourceMapping sourceMapping) {
        this.outputDir = outputDir.getAbsolutePath();
        this.analyzer = analyzer;
        this.accumulator = accumulator;
        this.sourceMapping = sourceMapping;
    }

    @Override
    public void visit(FileInfo file) {
        if (!file.getName().endsWith(".class")) return;
        process(file);
        try (InputStream is = file.open()) {
            ClassAnalysis classAnalysis = analyzer.getClassAnalysis(is);
            accumulator.addClass(classAnalysis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void process(FileInfo file) {
        if (!file.getName().contains("$")) return;

        sourceMapping.addTarget(outputDir, file.rawFile());
    }
}
