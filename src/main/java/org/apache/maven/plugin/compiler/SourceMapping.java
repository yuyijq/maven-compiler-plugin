package org.apache.maven.plugin.compiler;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class SourceMapping {

    public final Map<String, Set<String>> sources;

    public final Map<String, Set<String>> sourceToTargets;

    public SourceMapping() {
        this.sources = new HashMap<>();
        this.sourceToTargets = new HashMap<>();
    }

    public SourceMapping(Map<String, Set<String>> sources, Map<String, Set<String>> sourceToTargets) {
        this.sources = sources;
        this.sourceToTargets = sourceToTargets;
    }

    public void add(String sourceRoot, String source) {
        Set<String> sourceSet = sources.computeIfAbsent(sourceRoot, k -> new HashSet<>());
        sourceSet.add(source);
    }

    public void addTarget(String root, File target) {
        String relativePath = Util.relative(target, root);
        String source = Util.replaceExtension(relativePath, ".class", ".java");
        String sourceRoot = null;
        for (Map.Entry<String, Set<String>> entry : sources.entrySet()) {
            if (entry.getValue().contains(source)) {
                sourceRoot = entry.getKey();
            }
        }
        if (sourceRoot == null) return;
        File sourceFile = new File(sourceRoot, source);
        if (sourceFile.exists()) return;

        Set<String> targets = sourceToTargets.computeIfAbsent(source, k -> new HashSet<>());
        targets.add(relativePath);
    }

    public static class Serializer {
        public SourceMapping read(Decoder decoder) throws Exception {
            Map<String, Set<String>> sources = new HashMap<>();
            Map<String, Set<String>> sourceToTargets = new HashMap<>();

            int size = decoder.readSmallInt();
            for (int i = 0; i < size; ++i) {
                String sourceRoot = decoder.readString();
                int sourceSetSize = decoder.readSmallInt();
                Set<String> sourceSet = new HashSet<>();
                for (int n = 0; n < sourceSetSize; n++) {
                    String source = decoder.readString();
                    sourceSet.add(source);

                    int targetsSize = decoder.readSmallInt();
                    if (targetsSize > 0) {
                        Set<String> targets = new HashSet<>();
                        for (int m = 0; m < targetsSize; ++m) {
                            targets.add(decoder.readString());
                        }
                        sourceToTargets.put(source, targets);
                    }
                }
                sources.put(sourceRoot, sourceSet);
            }
            return new SourceMapping(sources, sourceToTargets);
        }

        public void write(Encoder encoder, SourceMapping value) throws Exception {
            Map<String, Set<String>> sources = value.sources;
            Map<String, Set<String>> sourceToTargets = value.sourceToTargets;
            encoder.writeSmallInt(sources.size());
            for (Map.Entry<String, Set<String>> entry : sources.entrySet()) {
                encoder.writeString(entry.getKey());
                Set<String> sourceSet = entry.getValue();
                encoder.writeSmallInt(sourceSet.size());
                for (String source : sourceSet) {
                    encoder.writeString(source);

                    Set<String> targets = sourceToTargets.get(source);
                    int size = (targets == null || targets.isEmpty()) ? 0 : targets.size();
                    encoder.writeSmallInt(size);

                    if (targets != null) {
                        for (String target : targets) {
                            encoder.writeString(target);
                        }
                    }
                }
            }
        }
    }
}
