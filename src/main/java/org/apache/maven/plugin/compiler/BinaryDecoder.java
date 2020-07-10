package org.apache.maven.plugin.compiler;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

class BinaryDecoder implements Decoder {
    private final InputStream is;

    public BinaryDecoder(InputStream is) {
        this.is = is;
    }

    @Override
    public int readSmallInt() throws IOException {
        byte[] bytes = new byte[Integer.BYTES];
        is.read(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return buffer.getInt();
    }

    @Override
    public IntSet readIntSet() throws IOException {
        int len = readSmallInt();
        IntSet result = new IntOpenHashSet(len);
        for (int i = 0; i < len; ++i) {
            result.add(readSmallInt());
        }
        return result;
    }

    @Override
    public String readNullableString() throws IOException {
        int len = readSmallInt();
        if (len == -1) {
            return null;
        }
        if (len == 0) {
            return "";
        }
        byte[] bytes = new byte[len];
        is.read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) is.read();
    }

    @Override
    public String readString() throws IOException {
        return readNullableString();
    }
}
