package org.apache.maven.plugin.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ClassPathEntryMapping {
    public final Map<String, Long> classPathEntries;

    public final Map<String, List<ZipEntryInfo>> zipEntries;

    public ClassPathEntryMapping(List<String> classPathEntries) {
        this.classPathEntries = new HashMap<>();
        this.zipEntries = new HashMap<>();
        if (classPathEntries != null && !classPathEntries.isEmpty()) {
            for (String classPathEntry : classPathEntries) {
                File file = new File(classPathEntry);
                if (!file.exists()) continue;
                this.classPathEntries.put(classPathEntry, file.lastModified());
            }
        }
    }

    public boolean isModified(String currentClassPathEntry) {
        Long time = classPathEntries.get(currentClassPathEntry);
        if (time == null) {
            return false;
        }

        long lastModified = new File(currentClassPathEntry).lastModified();
        if (lastModified > time) {
            return true;
        }

        return false;
    }

    private ClassPathEntryMapping(Map<String, Long> classPathEntries, Map<String, List<ZipEntryInfo>> zipEntries) {
        this.classPathEntries = classPathEntries;
        this.zipEntries = zipEntries;
    }

    public void add(String entry, List<ZipEntryInfo> list) {
        this.zipEntries.put(entry, list);
    }

    public static class Serializer {
        public ClassPathEntryMapping read(Decoder decoder) throws Exception {
            int len = decoder.readSmallInt();
            Map<String, Long> result = new HashMap<>();
            Map<String, List<ZipEntryInfo>> zipEntries = new HashMap<>();
            for (int i = 0; i < len; ++i) {
                String key = decoder.readString();
                Long time = Long.valueOf(decoder.readString());
                int entriesSize = decoder.readSmallInt();
                if (entriesSize > 0) {
                    List<ZipEntryInfo> list = new ArrayList<>(entriesSize);
                    for (int n = 0; n < entriesSize; ++n) {
                        String name = decoder.readString();
                        String className = decoder.readString();
                        Long hashCode = Long.valueOf(decoder.readString());
                        list.add(new ZipEntryInfo(name, className, hashCode));
                    }
                    zipEntries.put(key, list);
                }
                result.put(key, time);
            }
            return new ClassPathEntryMapping(result, zipEntries);
        }

        public void write(Encoder encoder, ClassPathEntryMapping value) throws Exception {
            Map<String, Long> map = value.classPathEntries;
            Map<String, List<ZipEntryInfo>> zipEntries = value.zipEntries;
            encoder.writeSmallInt(map.size());
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                encoder.writeString(entry.getKey());
                encoder.writeString(String.valueOf(entry.getValue()));
                List<ZipEntryInfo> list = zipEntries.get(entry.getKey());
                int entriesSize = list == null ? 0 : list.size();
                encoder.writeSmallInt(entriesSize);
                if (entriesSize > 0) {
                    for (ZipEntryInfo zipEntryInfo : list) {
                        encoder.writeString(zipEntryInfo.name);
                        encoder.writeString(zipEntryInfo.className);
                        encoder.writeString(String.valueOf(zipEntryInfo.hashCode));
                    }
                }

            }
        }
    }
}
