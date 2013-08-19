/*******************************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc-Andre Laperle - Initial implementation
 *******************************************************************************/

package org.eclipse.linuxtools.internal.tmf.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

@SuppressWarnings("javadoc")
public final class IndexHelper {
    final static String CHARSET_NAME = "UTF-16"; //$NON-NLS-1$

    public static void writeLong(OutputStream stream, long value) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8).putLong(value);
        stream.write(byteBuffer.array());
    }

    public static void writeInt(OutputStream stream, int value) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4).putInt(value);
        stream.write(byteBuffer.array());
    }

    public static long readLong(InputStream stream) throws IOException {
        byte arr[] = new byte[8];
        stream.read(arr);
        return ByteBuffer.wrap(arr).getLong();
    }

    public static int readInt(InputStream stream) throws IOException {
        byte arr[] = new byte[4];
        stream.read(arr);
        return ByteBuffer.wrap(arr).getInt();
    }

    public static String readString(InputStream stream) throws IOException {
        int length = readInt(stream);
        byte arr[] = new byte[length * 2];
        stream.read(arr);
        return new String (arr, Charset.forName(CHARSET_NAME));
    }

    public static void writeString(OutputStream stream, String value) throws IOException {
        writeInt(stream, value.length() * 2);
        stream.write(Charset.forName(CHARSET_NAME).encode(value).array());
    }


    public static void writeLong(ByteBuffer buffer, long value) {
        buffer.putLong(value);
    }

    public static void writeInt(ByteBuffer buffer, int value) {
        buffer.putInt(value);
    }

    public static long readLong(ByteBuffer buffer) {
        return buffer.getLong();
    }

    public static int readInt(ByteBuffer buffer) {
        return buffer.getInt();
    }

}

