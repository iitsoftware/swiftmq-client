/*
 * Copyright 2019 IIT Software GmbH
 *
 * IIT Software GmbH licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.swiftmq.tools.util;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class LazyUTF8String implements Serializable {
    private AtomicReference<String> s = new AtomicReference<>();
    private AtomicReference<byte[]> buffer = new AtomicReference<>();
    private int utfLength;

    public LazyUTF8String(DataInput in) throws IOException {
        utfLength = in.readUnsignedShort();
        byte[] b = new byte[utfLength + 2];
        in.readFully(b, 2, utfLength);
        b[0] = (byte) ((utfLength >>> 8) & 0xFF);
        b[1] = (byte) ((utfLength) & 0xFF);
        buffer.set(b);
    }

    public LazyUTF8String(String s) {
        try {
            if (s == null) {
                System.out.println("s==null");
                throw new NullPointerException();

            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            throw e;
        }
        this.s.set(s);
    }

    private String bufferToString() throws Exception {
        return UTFUtils.convertFromUTF8(buffer.get(), 2, utfLength);
    }

    private byte[] stringToBuffer() throws Exception {
        utfLength = UTFUtils.countUTFBytes(s.get());
        if (utfLength > 65535)
            throw new UTFDataFormatException();

        byte[] b = new byte[utfLength + 2];
        int count = 0;
        count = UTFUtils.writeShortToBuffer(utfLength, b, count);
        UTFUtils.writeUTFBytesToBuffer(s.get(), b, count);
        return b;
    }

    public String getString() {
        return getString(false);
    }

    public String getString(boolean clear) {
        try {
            String currentString = s.get();
            if (currentString == null) {
                currentString = bufferToString();
                if (s.compareAndSet(null, currentString)) {
                    if (clear) {
                        buffer.set(null);
                        utfLength = 0;
                    }
                } else {
                    currentString = s.get();
                }
            }
            return currentString;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getBuffer() {
        try {
            byte[] currentBuffer = buffer.get();
            if (currentBuffer == null) {
                currentBuffer = stringToBuffer();
                if (!buffer.compareAndSet(null, currentBuffer)) {
                    currentBuffer = buffer.get();
                }
            }
            return currentBuffer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeContent(DataOutput out) throws IOException {
        out.write(getBuffer());
    }

    public String toString() {
        return "[LazyUTF8String, s=" + s.get() + ", buffer=" + Arrays.toString(buffer.get()) + "]";
    }

}
