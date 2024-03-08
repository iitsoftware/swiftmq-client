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

package com.swiftmq.mgmt;

import com.swiftmq.tools.dump.Dumpable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;


public class MetaData implements Dumpable {
    final AtomicReference<String> name = new AtomicReference<String>();
    final AtomicReference<String> displayName = new AtomicReference<String>();
    final AtomicReference<String> vendor = new AtomicReference<String>();
    final AtomicReference<String> version = new AtomicReference<String>();
    final AtomicReference<String> className = new AtomicReference<String>();
    final AtomicReference<String> description = new AtomicReference<String>();

    public MetaData(String displayName, String vendor, String version, String description) {
        this.displayName.set(displayName);
        this.vendor.set(vendor);
        this.version.set(version);
        this.description.set(description);
    }

    MetaData() {
    }

    public int getDumpId() {
        return MgmtFactory.META;
    }

    private void writeDump(DataOutput out, String s) throws IOException {
        if (s == null)
            out.writeByte(0);
        else {
            out.writeByte(1);
            out.writeUTF(s);
        }
    }

    private String readDump(DataInput in) throws IOException {
        byte set = in.readByte();
        if (set == 1)
            return in.readUTF();
        return null;
    }

    public void writeContent(DataOutput out)
            throws IOException {
        writeDump(out, displayName.get());
        writeDump(out, vendor.get());
        writeDump(out, version.get());
        writeDump(out, description.get());
        writeDump(out, className.get());
    }

    public void readContent(DataInput in)
            throws IOException {
        displayName.set(readDump(in));
        vendor.set(readDump(in));
        version.set(readDump(in));
        description.set(readDump(in));
        className.set(readDump(in));
    }

    public String getName() {
        return (name.get());
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getDisplayName() {
        return (displayName.get());
    }

    public String getVendor() {
        return (vendor.get());
    }

    public String getVersion() {
        return (version.get());
    }

    public String getClassName() {
        return (className.get());
    }

    public void setClassName(String className) {
        this.className.set(className);
    }

    public String getDescription() {
        return (description.get());
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("[MetaData, name=");
        s.append(name.get());
        s.append(", displayName=");
        s.append(displayName.get());
        s.append(", description=");
        s.append(description.get());
        s.append(", version=");
        s.append(version.get());
        s.append(", vendor=");
        s.append(vendor.get());
        s.append(", className=");
        s.append(className.get());
        s.append("]");
        return s.toString();
    }
}

