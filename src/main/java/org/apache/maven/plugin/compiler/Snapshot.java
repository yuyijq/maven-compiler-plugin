package org.apache.maven.plugin.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

class Snapshot {

    private final File outputDir;
    private final File statusDir;
    private final List<String> sourceRoots;
    private final List<String> dependencyJars;

    public Snapshot(File outputDir, File statusDir, List<String> sourceRoots, List<String> dependencyJars) {
        this.outputDir = outputDir;
        this.statusDir = statusDir;
        this.sourceRoots = sourceRoots;
        this.dependencyJars = dependencyJars;
    }

    public void capture() {
        StringInterner interner = new StringInterner();
        ClassDependenciesAnalyzer analyzer = new ClassDependenciesAnalyzer(interner);
        ClassDependentsAccumulator accumulator = new ClassDependentsAccumulator();

        ClassPathEntryMapping classPathEntryMapping = new ClassPathEntryMapping(dependencyJars);
        ZipFileScanner zipFileScanner = new ZipFileScanner();
        List<ZipEntryInfo> zipEntries = new ArrayList<>();
        for (String classPathEntry : dependencyJars) {
            File file = new File(classPathEntry);
            zipFileScanner.scan(file, new ZipFileVisitor(analyzer, accumulator, zipEntries));
            classPathEntryMapping.add(classPathEntry, zipEntries);
        }

        SourceMapping sourceMapping = new SourceMapping();
        SourceScanner sourceScanner = new SourceScanner(sourceMapping);
        sourceScanner.scan(sourceRoots);

        ClassPathEntryMapping.Serializer classPathEntryMappingSerializer = new ClassPathEntryMapping.Serializer();
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.scan(outputDir, new ClassAnalysisFileVisitor(outputDir, analyzer, accumulator, sourceMapping));

        ClassSetAnalysisData.Serializer serializer = new ClassSetAnalysisData.Serializer(interner);
        SourceMapping.Serializer sourceMappingSerializer = new SourceMapping.Serializer();
        File snapshot = new File(statusDir, "snapshot");
        try (OutputStream end = new FileOutputStream(snapshot)) {
            ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
            BinaryEncoder encoder = new BinaryEncoder(os);
            classPathEntryMappingSerializer.write(encoder, classPathEntryMapping);
            sourceMappingSerializer.write(encoder, sourceMapping);
            serializer.write(encoder, accumulator.getAnalysis());
            os.flush();
            end.write(os.toByteArray());
            end.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
