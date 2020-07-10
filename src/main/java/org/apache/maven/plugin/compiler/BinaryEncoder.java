package org.apache.maven.plugin.compiler;

import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

class BinaryEncoder implements Encoder {

    private final OutputStream os;

    public BinaryEncoder(OutputStream os) {
        this.os = os;
    }

    @Override
    public void writeSmallInt(int value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(value);
        buffer.flip();
        os.write(buffer.array());
    }

    @Override
    public void writeIntSet(IntSet value) throws IOException {
        int len = value.size();
        writeSmallInt(len);
        for (Integer i : value) {
            writeSmallInt(i);
        }
    }

    @Override
    public void writeNullableString(String value) throws IOException {
        if (value == null) {
            writeSmallInt(-1);
        } else {
            int len = value.length();
            writeSmallInt(len);
            os.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void writeByte(byte value) throws IOException {
        os.write(value);
    }

    @Override
    public void writeString(String value) throws IOException {
        writeNullableString(value);
    }
}
