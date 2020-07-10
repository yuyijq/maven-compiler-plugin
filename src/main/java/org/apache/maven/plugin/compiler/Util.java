package org.apache.maven.plugin.compiler;

import java.io.File;

class Util {
    public static String relative(File file, String root) {
        String absolutePath = file.getAbsolutePath();
        if (!root.endsWith(File.separator)) {
            root = root + File.separator;
        }
        return absolutePath.substring(root.length());
    }

    public static String replaceExtension(String source, String originExtension, String targetExtension) {
        int index = source.indexOf(originExtension);
        if (index < 0) {
            return source;
        }
        source = source.substring(0, index) + targetExtension;
        return source;
    }
}
