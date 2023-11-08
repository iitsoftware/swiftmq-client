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

package com.swiftmq.tools.collection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FragmentedOutputStream {
    private final List<Fragment> fragments = new ArrayList<>();
    private final int fragmentSize;
    private boolean includeLength;
    private int totalLength = 0;

    public FragmentedOutputStream(int fragmentSize, boolean includeLength) {
        if (fragmentSize <= 0) {
            throw new IllegalArgumentException("Fragment size must be greater than 0");
        }
        this.fragmentSize = fragmentSize;
        this.includeLength = includeLength;
        // Initialize the first fragment, possibly leaving space for the length prefix
        fragments.add(new Fragment(new byte[fragmentSize], includeLength ? 4 : 0));
        totalLength = includeLength ? 4 : 0;
    }

    private void ensureCapacity(int length) {
        if (fragments.isEmpty() || fragments.get(fragments.size() - 1).isFull()) {
            fragments.add(new Fragment(new byte[fragmentSize], 0));
        }
    }

    public void write(int b) throws IOException {
        ensureCapacity(1);
        Fragment currentFragment = fragments.get(fragments.size() - 1);
        currentFragment.data[currentFragment.length++] = (byte) b;
        if (includeLength) {
            totalLength++;
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        int bytesWritten = 0;
        while (bytesWritten < len) {
            ensureCapacity(len - bytesWritten);
            Fragment currentFragment = fragments.get(fragments.size() - 1);
            int bytesToWrite = Math.min(len - bytesWritten, fragmentSize - currentFragment.length);
            System.arraycopy(b, off + bytesWritten, currentFragment.data, currentFragment.length, bytesToWrite);
            bytesWritten += bytesToWrite;
            currentFragment.length += bytesToWrite;
            if (includeLength) {
                totalLength += bytesToWrite;
            }
        }
    }

    public void finish() {
        if (includeLength) {
            // Write the total length at the beginning of the first fragment
            ByteBuffer.wrap(fragments.get(0).data).putInt(0, totalLength);
        }
    }

    public void reset() {
        // Clear the existing fragments
        fragments.clear();
        // Add a new Fragment, with space for length if required
        fragments.add(new Fragment(new byte[fragmentSize], includeLength ? 4 : 0));
        // Reset total size, including the space for the length prefix if necessary
        totalLength = includeLength ? 4 : 0;
    }

    public int getFragmentCount() {
        return fragments.size();
    }

    public Fragment getFragment(int index) {
        if (index < 0 || index >= getFragmentCount()) {
            throw new IndexOutOfBoundsException("Fragment index out of range: " + index);
        }
        return fragments.get(index);
    }

    public class Fragment {
        byte[] data;
        int length;

        public Fragment(byte[] data, int length) {
            this.data = data;
            this.length = length;
        }

        public boolean isFull() {
            return length == data.length;
        }
    }

    // Additional methods to manage fragments, reset the stream, etc., can be added here
}
