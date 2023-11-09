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

import com.swiftmq.tools.gc.WeakPool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * The FragmentedOutputStream class is designed for efficient network transmission
 * by breaking down data into manageable fragments. It provides an output stream that
 * collects written data into fixed-size byte arrays, referred to as fragments.
 * This approach is particularly useful for network protocols that require data to be
 * sent in discrete chunks and can help to align with network packet sizes to optimize transmission.
 * <p>
 * The class offers functionality to write data byte by byte or in byte arrays, manage the size of each fragment,
 * and to optionally include the overall length of the data at the beginning of the stream.
 * It leverages an internal pooling mechanism to minimize garbage collection overhead by reusing byte arrays
 * where possible. This is particularly useful in high-throughput scenarios where stream reset and reuse are common.
 */
public class FragmentedOutputStream {
    private final List<Fragment> fragments = new ArrayList<>();
    private final WeakPool<Fragment> fragmentPool = new WeakPool<>();
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
        firstFragment();
        totalLength = 0; // without the length field
    }

    private void firstFragment() {
        Fragment fragment = fragmentPool.get(() -> new Fragment(new byte[fragmentSize], 0));
        fragment.length = includeLength ? 4 : 0;
        fragments.add(fragment);
    }

    private void ensureCapacity(int length) {
        if (fragments.isEmpty() || fragments.get(fragments.size() - 1).isFull()) {
            fragments.add(fragmentPool.get(() -> new Fragment(new byte[fragmentSize], 0)));
        }
    }

    public Fragment getCurrentFragment() {
        return fragments.get(fragments.size() - 1);
    }

    public void write(int b) {
        ensureCapacity(1);
        Fragment currentFragment = getCurrentFragment();
        currentFragment.data[currentFragment.length++] = (byte) b;
        if (includeLength) {
            totalLength++;
        }
    }

    public void write(byte[] b, int off, int len) {
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
            Fragment currentFragment = getCurrentFragment();
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
        // Clear the existing fragments and checkin to the pool
        fragments.forEach(f -> {
            f.length = 0;
            fragmentPool.checkIn(f);
        });
        fragments.clear();
        // Add a new Fragment, with space for length if required
        firstFragment();
        // Reset total size
        totalLength = 0;
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

        public byte[] getData() {
            return data;
        }

        public int getLength() {
            return length;
        }

        public boolean isFull() {
            return length == data.length;
        }
    }

    // Additional methods to manage fragments, reset the stream, etc., can be added here
}
