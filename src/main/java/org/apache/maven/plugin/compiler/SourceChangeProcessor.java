package org.apache.maven.plugin.compiler;

import org.apache.commons.lang.math.NumberUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.maven.plugin.compiler.Util.replaceExtension;

class SourceChangeProcessor {
    private final File snapshot;

    private boolean needRebuildAll;

    public SourceChangeProcessor(File statusDir, boolean isIncrCompile) {
        this.snapshot = new File(statusDir, "snapshot");
        this.needRebuildAll = !isIncrCompile || !snapshot.exists();
    }

    public List<FileChange> process(List<String> sourceRoot, File targetDir, List<String> classPathEntries) {
        if (needRebuildAll) {
            return Collections.emptyList();
        }

        List<FileChange> fileChanges = new ArrayList<>();
        try (FileInputStream is = new FileInputStream(snapshot)) {
            BinaryDecoder decoder = new BinaryDecoder(is);
            ClassPathEntryMapping.Serializer classPathEntryMappingSerializer = new ClassPathEntryMapping.Serializer();
            ClassPathEntryMapping classPathEntryMapping = classPathEntryMappingSerializer.read(decoder);
            SourceMapping.Serializer serializer = new SourceMapping.Serializer();
            SourceMapping sourceMapping = serializer.read(decoder);

            StringInterner interner = new StringInterner();
            ClassSetAnalysisData.Serializer classSetSerializer = new ClassSetAnalysisData.Serializer(interner);
            ClassSetAnalysisData classSetAnalysisData = classSetSerializer.read(decoder);

            Set<String> vissited = new HashSet<>();
            Set<String> changedClasses = new HashSet<>();

            Map<String, Set<String>> sources = sourceMapping.sources;
            Map<String, Set<String>> sourceToTargets = sourceMapping.sourceToTargets;

            Set<String> newSourceSet = new HashSet<>();
            for (String dir : sourceRoot) {
                DirectoryScanner directoryScanner = new DirectoryScanner();
                File start = new File(dir);
                Set<String> sourceSet = sources.get(dir);

                directoryScanner.scan(start, file -> {
                    if (!file.getName().endsWith(".java")) return;

                    String sourceFile = Util.relative(file.rawFile(), dir);
                    newSourceSet.add(sourceFile);

                    if (!sourceSet.contains(sourceFile)) {
                        vissited.add(sourceFile);
                        fileChanges.add(new FileChange(file.rawFile(), ChangeType.Add));
                    } else {
                        Set<String> targets = mapToTargets(sourceToTargets, sourceFile);
                        boolean changed = false;
                        for (String target : targets) {
                            File targetFile = new File(targetDir, target);
                            if (!targetFile.exists()) {
                                fileChanges.add(new FileChange(file.rawFile(), ChangeType.Add));
                            } else {
                                if (file.lastModified() > targetFile.lastModified()) {
                                    changed = true;
                                }
                            }

                        }
                        if (changed) {
                            List<String> list = targets.stream().map(x -> toClassName(x)).collect(Collectors.toList());
                            changedClasses.addAll(list);
                            vissited.add(sourceFile);
                            fileChanges.add(new FileChange(file.rawFile(), ChangeType.Mod));
                        }
                    }
                });

                Set<String> oldSourceSet = sources.get(dir);
                if (oldSourceSet == null) continue;
                for (String source : oldSourceSet) {
                    if (!newSourceSet.contains(source)) {
                        vissited.add(source);
                        Set<String> targets = mapToTargets(sourceToTargets, source);
                        for (String target : targets) {
                            fileChanges.add(new FileChange(new File(targetDir, target), ChangeType.Deleted));
                        }
                    }
                }
            }

            ZipFileScanner zipFileScanner = new ZipFileScanner();
            for (String classPathEntry : classPathEntries) {
                if (classPathEntryMapping.isModified(classPathEntry)) {
                    File file = new File(classPathEntry);

                    List<ZipEntryInfo> currentZipEntries = new ArrayList<>();
                    ZipFileVisitor zipFileVisitor = new ZipFileVisitor(currentZipEntries);
                    zipFileScanner.scan(file, zipFileVisitor);

                    Map<String, Long> entryToHashCode = new HashMap<>();
                    for (ZipEntryInfo currentZipEntry : currentZipEntries) {
                        entryToHashCode.put(currentZipEntry.name, currentZipEntry.hashCode);
                    }

                    List<ZipEntryInfo> oldZipEntries = classPathEntryMapping.zipEntries.get(classPathEntry);
                    if (oldZipEntries == null) continue;

                    for (ZipEntryInfo zipEntry : oldZipEntries) {
                        Long newHashCode = entryToHashCode.get(zipEntry);
                        if (newHashCode == null) continue;
                        if (newHashCode.equals(zipEntry.hashCode)) continue;

                        changedClasses.add(zipEntry.className);
                    }
                }
            }
            Set<String> visitedClasses = new HashSet<>();
            Set<String> direct = new HashSet<>();

            //A修改了，B和C直接依赖A，B的public API依赖A，C的内部实现依赖A
            //A, B和C都要重新编译
            //但是因为C的public API不依赖A，所以依赖C的类不用重新编译
            //B的public API依赖A，那么B的
            //changedClasses 是本次编译有修改的类，这里先找出直接依赖它的类，这个时候不管是public依赖还是private依赖都要收集
            for (String changedClass : changedClasses) {
                DependentsSet dependents = classSetAnalysisData.getDependents(changedClass);
                direct.addAll(dependents.getAllDependentClasses());
            }

            //下一步是处理间接依赖的类
            for (String changedClass : direct) {
                resolveInDirectDependents(sources, classSetAnalysisData, fileChanges, vissited, changedClass, visitedClasses);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return fileChanges;
    }

    private Set<String> mapToTargets(Map<String, Set<String>> sourceToTargets, String source) {
        Set<String> targets = sourceToTargets.get(source);
        if (targets == null) {
            Set<String> result = new HashSet<>();
            String target = replaceExtension(source, ".java", ".class");
            result.add(target);
            return result;
        }
        return targets;
    }


    private String toClassName(String changedClassFile) {
        int index = changedClassFile.indexOf(".class");
        changedClassFile = changedClassFile.substring(0, index);
        return changedClassFile.replace('/', '.');
    }

    private void resolveInDirectDependents(Map<String, Set<String>> sources,
                                           ClassSetAnalysisData classSetAnalysisData,
                                           List<FileChange> fileChanges,
                                           Set<String> visited,
                                           String changedClass,
                                           Set<String> visitedClasses) {
        if (visitedClasses.contains(changedClass)) return;
        visitedClasses.add(changedClass);

        String source = mapToSource(changedClass);
        if (!visited.contains(source)) {
            visited.add(source);
            String sourceRoot = getSourceRoot(sources, source);
            if (sourceRoot != null) {
                File file = new File(sourceRoot, source);
                if (file.exists()) {
                    fileChanges.add(new FileChange(file, ChangeType.Mod));
                }
            }
        }
        DependentsSet dependents = classSetAnalysisData.getDependents(changedClass);
        Set<String> allDependentClasses = dependents.getAccessibleDependentClasses();
        if (allDependentClasses == null || allDependentClasses.isEmpty()) return;
        for (String dependentClass : allDependentClasses) {
            resolveInDirectDependents(sources, classSetAnalysisData, fileChanges, visited, dependentClass, visitedClasses);
        }
    }

    private String getSourceRoot(Map<String, Set<String>> sources, String source) {
        for (Map.Entry<String, Set<String>> entry : sources.entrySet()) {
            if (entry.getValue().contains(source)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String mapToSource(String target) {
        int index = target.lastIndexOf('$');
        if (index > 0) {
            String suffix = target.substring(index + 1);
            if (NumberUtils.isDigits(suffix)) {
                target = target.substring(0, index);
            }
        }
        target = target.replace('.', '/');
        target = target + ".java";
        return target;
    }

    public boolean isNeedRebuildAll() {
        return needRebuildAll;
    }
}
