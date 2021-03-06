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

package com.swiftmq.jms.primitives;

import com.swiftmq.tools.dump.Dumpable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class _Bytes implements Dumpable, Primitive {
    byte[] value = null;

    public _Bytes() {
    }

    public _Bytes(byte[] value) {
        this.value = new byte[value.length];
        System.arraycopy(value, 0, this.value, 0, value.length);
    }

    public _Bytes(byte[] value, int offset, int length) {
        this.value = new byte[length];
        System.arraycopy(value, offset, this.value, 0, length);
    }

    public byte[] bytesValue() {
        return value;
    }

    public Object getObject() {
        return value;
    }

    public int getDumpId() {
        return BYTES;
    }

    public void writeContent(DataOutput out)
            throws IOException {
        out.writeInt(value.length);
        out.write(value);
    }

    public void readContent(DataInput in)
            throws IOException {
        value = new byte[in.readInt()];
        in.readFully(value);
    }

    public String toString() {
        return value.toString();
    }
}
