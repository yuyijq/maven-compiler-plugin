package org.apache.maven.plugin.compiler;

class ZipEntryInfo {
    public final String name;

    public final String className;

    public final long hashCode;

    public ZipEntryInfo(String name, String className, long hashCode) {
        this.name = name;
        this.className = className;
        this.hashCode = hashCode;
    }
}
