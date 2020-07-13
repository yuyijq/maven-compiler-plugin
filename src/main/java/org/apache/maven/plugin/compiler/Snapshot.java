package org.apache.maven.plugin.compiler;

public class Snapshot {
    public final ClassPathEntryMapping classPathEntryMapping;

    public final SourceMapping sourceMapping;

    public final ClassSetAnalysisData classSetAnalysisData;

    public Snapshot(ClassPathEntryMapping classPathEntryMapping, SourceMapping sourceMapping, ClassSetAnalysisData classSetAnalysisData) {
        this.classPathEntryMapping = classPathEntryMapping;
        this.sourceMapping = sourceMapping;
        this.classSetAnalysisData = classSetAnalysisData;
    }
}
