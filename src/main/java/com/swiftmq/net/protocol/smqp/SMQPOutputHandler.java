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

import com.swiftmq.net.protocol.ProtocolOutputHandler;
import com.swiftmq.net.protocol.util.FragmentedOutputStream;

/**
 * A SMQPOutputHandler handles SMQP output.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2002, All Rights Reserved
 * @author IIT GmbH, Muenster/Germany, 08 Oct 2023, Changed to Fragments
 */

public class SMQPOutputHandler extends ProtocolOutputHandler {
    private final FragmentedOutputStream fragmentedOutputStream;
    private FragmentedOutputStream.Fragment currentFragment = null;
    private int currentOffset = 0;
    private int chunkCount = 0;
    private int currentFragmentIndex = 0;

    public SMQPOutputHandler(int bufferSize, int extendSize) {
        fragmentedOutputStream = new FragmentedOutputStream(bufferSize, true);
    }

    @Override
    public ProtocolOutputHandler create(int bufferSize, int extendSize) {
        return new SMQPOutputHandler(bufferSize, extendSize);
    }

    public int getChunkCount() {
        return chunkCount;
    }

    protected byte[] getByteArray() {
        return currentFragment != null ? currentFragment.getData() : null;
    }

    protected int getOffset() {
        return currentOffset;
    }

    protected int getLength() {
        int length = currentFragment != null ? currentFragment.getLength() - currentOffset : 0;
        return length;
    }

    protected void setBytesWritten(int written) {
        if (currentFragment != null) {
            currentOffset += written;

            if (currentOffset >= currentFragment.getLength()) {
                currentOffset = 0;
                currentFragmentIndex++;
                if (currentFragmentIndex >= fragmentedOutputStream.getFragmentCount()) {
                    resetFragmentedOutputStream();
                } else {
                    currentFragment = fragmentedOutputStream.getFragment(currentFragmentIndex);
                    chunkCount--;
                }
            }
        }
    }

    private void resetFragmentedOutputStream() {
        fragmentedOutputStream.reset();
        currentFragment = null;
        currentFragmentIndex = 0;
        currentOffset = 0;
        chunkCount = 0;
    }

    protected void addByte(byte b) {
        fragmentedOutputStream.write(b);
    }

    protected void addBytes(byte[] b, int offset, int len) {
        fragmentedOutputStream.write(b, offset, len);
    }

    protected void markChunkCompleted() {
        fragmentedOutputStream.finish();
        currentFragment = fragmentedOutputStream.getFragment(0);
        currentOffset = 0;
        chunkCount = fragmentedOutputStream.getFragmentCount();
    }

    public String toString() {
        return "[SMQPOutputHandler, chunkCount=" + getChunkCount() + "]";
    }
}
