package org.apache.maven.plugin.compiler;

import com.google.common.io.ByteStreams;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class Test {
    public static void main(String[] args) throws Exception {
//        BinaryDecoder decoder = new BinaryDecoder(new FileInputStream("/Users/yuzhaohui/work/wclient/wclient-remoting/target/maven-status/maven-compiler-plugin/compile/default-compile/snapshot"));
//        SourceMapping.Serializer serializer = new SourceMapping.Serializer();
//        SourceMapping sourceMapping = serializer.read(decoder);
//        ClassSetAnalysisData.Serializer serializer2 = new ClassSetAnalysisData.Serializer(new StringInterner());
//        ClassSetAnalysisData data = serializer2.read(decoder);
//        System.out.println(data);

        InputStream input = new FileInputStream(new File("/Users/yuzhaohui/opensource/maven-compiler-plugin/target/classes/org/apache/maven/plugin/compiler/A.class"));
        ClassReader reader = new ClassReader(ByteStreams.toByteArray(input));
        String className = reader.getClassName().replace("/", ".");

        ClassAnalysis classAnalysis = ClassDependenciesVisitor.analyze(className, reader, new StringInterner());
        System.out.println(className);

        ClassDependentsAccumulator accumulator = new ClassDependentsAccumulator();
        accumulator.addClass(classAnalysis);

        ClassSetAnalysisData analysisData = accumulator.getAnalysis();
        DependentsSet dependents = analysisData.getDependents("org.apache.maven.plugin.compiler.B");
        System.out.println(dependents);
    }
}
