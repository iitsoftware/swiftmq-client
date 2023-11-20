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

package com.swiftmq.net.protocol.smqp;

import com.swiftmq.net.protocol.ChunkListener;
import com.swiftmq.net.protocol.ProtocolInputHandler;

import java.nio.ByteBuffer;

/**
 * A SMQPInputHandler handles SMQP input.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2002, All Rights Reserved
 */
public class SMQPInputHandler implements ProtocolInputHandler {
    ChunkListener listener = null;
    byte[] lengthField = new byte[4];
    byte[] buffer = null;
    byte[] prevBuffer = null;
    ByteBuffer byteBuffer = null;
    int bufferOffset = 0;
    boolean lengthComplete = false;
    int lengthByteCount = 0;
    int chunkLength = 0;

    public ProtocolInputHandler create() {
        return new SMQPInputHandler();
    }

    public void setChunkListener(ChunkListener listener) {
        this.listener = listener;
    }

    public void createInputBuffer(int initialSize, int ensureSize) {
        // Initialize the lengthField buffer and the associated ByteBuffer.
        buffer = lengthField;
        byteBuffer = ByteBuffer.wrap(buffer);
        // Reset the offset to 0 as we are starting to read a new length field.
        bufferOffset = 0;
        // Reset the byte count for the length field as we haven't read any part of the length yet.
        lengthByteCount = 0;
        // Indicate that we're not complete with the length field.
        lengthComplete = false;
    }

    public ByteBuffer getByteBuffer() {
        byteBuffer.position(bufferOffset);
        return byteBuffer;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getOffset() {
        return bufferOffset;
    }

    private int readLength(byte[] b, int offset) {
        int i1 = b[offset] & 0xff;
        int i2 = b[offset + 1] & 0xff;
        int i3 = b[offset + 2] & 0xff;
        int i4 = b[offset + 3] & 0xff;
        int i = (i1 << 24) + (i2 << 16) + (i3 << 8) + (i4 << 0);
        return i;
    }

    public void setBytesWritten(int written) {
        if (lengthComplete) {
            bufferOffset += written;
            // If we have read as many bytes as the chunkLength, the chunk is complete.
            if (bufferOffset == chunkLength) {
                listener.chunkCompleted(buffer, 0, chunkLength);
                lengthComplete = false;
                lengthByteCount = 0;
                bufferOffset = 0; // Reset bufferOffset for the next length/chunk read.
                prevBuffer = buffer;
                buffer = lengthField;
                byteBuffer = ByteBuffer.wrap(buffer);
            }
        } else {
            lengthByteCount += written;
            bufferOffset += written;
            // Check if we have completed the length field
            if (lengthByteCount == 4) {
                chunkLength = readLength(buffer, 0); // Assuming lengthFieldPos is always 0 here
                if (prevBuffer != null && prevBuffer.length == chunkLength)
                    buffer = prevBuffer;
                else
                    buffer = new byte[chunkLength];
                byteBuffer = ByteBuffer.wrap(buffer);
                lengthComplete = true;
                bufferOffset = 0; // Reset bufferOffset for the chunk read.
                lengthByteCount = 0; // Reset lengthByteCount for the next length read.
            }
        }
    }

    public String toString() {
        return "[SMQPInputHandler]";
    }
}

