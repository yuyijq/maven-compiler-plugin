package org.apache.maven.plugin.compiler;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

class SnapshotCapture {

    private final File outputDir;
    private final File statusDir;
    private final List<String> sourceRoots;
    private final List<String> dependencyJars;

    private final List<String> outputDirs;

    public SnapshotCapture(File outputDir, File statusDir, List<String> sourceRoots, List<String> dependencyJars, MavenSession session) {
        this.outputDir = outputDir;
        this.statusDir = statusDir;
        this.sourceRoots = sourceRoots;
        this.dependencyJars = dependencyJars;
        this.outputDirs = new ArrayList<>();
        List<MavenProject> projects = session.getProjects();
        for (MavenProject project : projects) {
            String outputDirectory = project.getBuild().getDirectory();
            outputDirs.add(outputDirectory);
        }
    }

    public Snapshot capture() {
        StringInterner interner = new StringInterner();
        ClassDependenciesAnalyzer analyzer = new ClassDependenciesAnalyzer(interner);
        ClassDependentsAccumulator accumulator = new ClassDependentsAccumulator();

        ClassPathEntryMapping classPathEntryMapping = new ClassPathEntryMapping(dependencyJars);
        ZipFileScanner zipFileScanner = new ZipFileScanner();
        List<ZipEntryInfo> zipEntries = new ArrayList<>();
        for (String classPathEntry : dependencyJars) {
            if (isSnapshot(classPathEntry) || isSameProject(classPathEntry)) {
                File file = new File(classPathEntry);
                zipFileScanner.scan(file, new ZipFileVisitor(analyzer, accumulator, zipEntries));
                classPathEntryMapping.add(classPathEntry, zipEntries);
            }
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
            BinaryEncoder encoder = new BinaryEncoder(end);
            classPathEntryMappingSerializer.write(encoder, classPathEntryMapping);
            sourceMappingSerializer.write(encoder, sourceMapping);
            serializer.write(encoder, accumulator.getAnalysis());
            end.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Snapshot(classPathEntryMapping, sourceMapping, accumulator.getAnalysis());
    }

    private boolean isSameProject(String jar) {
        for (String dir : outputDirs) {
            if (jar.startsWith(dir)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSnapshot(String jar) {
        return jar.contains("SNAPSHOT");
    }
}
