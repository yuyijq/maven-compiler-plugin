package org.apache.maven.plugin.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

interface FileInfo {

    String getName();

    void delete();

    InputStream open() throws IOException;

    long lastModified();

    File rawFile();
}
