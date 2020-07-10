package org.apache.maven.plugin.compiler;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;

import java.util.Set;

class ClassAnalysis {
    private final String className;
    private final Set<String> privateClassDependencies;
    private final Set<String> accessibleClassDependencies;
    private final boolean dependencyToAll;
    private final IntSet constants;

    public ClassAnalysis(String className,
                         Set<String> privateClassDependencies,
                         Set<String> accessibleClassDependencies,
                         boolean dependencyToAll,
                         IntSet constants) {
        this.className = className;
        this.privateClassDependencies = ImmutableSet.copyOf(privateClassDependencies);
        this.accessibleClassDependencies = ImmutableSet.copyOf(accessibleClassDependencies);
        this.dependencyToAll = dependencyToAll;
        this.constants = constants.isEmpty() ? IntSets.EMPTY_SET : constants;
    }

    public String getClassName() {
        return className;
    }

    public Set<String> getPrivateClassDependencies() {
        return privateClassDependencies;
    }

    public Set<String> getAccessibleClassDependencies() {
        return accessibleClassDependencies;
    }

    public IntSet getConstants() {
        return constants;
    }

    public boolean isDependencyToAll() {
        return dependencyToAll;
    }
}
