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
package com.swiftmq.tools.concurrent;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicWrappingCounterLong {
    private final AtomicReference<Long> counter;
    private final long initialValue;
    private final long maxValue;
    public AtomicWrappingCounterLong(long initialValue) {
        this(initialValue, Long.MAX_VALUE);
    }

    public AtomicWrappingCounterLong(long initialValue, long maxValue) {
        this.initialValue = initialValue;
        this.maxValue = maxValue;
        this.counter = new AtomicReference<>(initialValue);
    }

    public long get() {
        return counter.get();
    }

    public long getAndIncrement() {
        return counter.getAndUpdate(current -> (current == maxValue) ? initialValue : current + 1);
    }

    public void reset() {
        counter.set(initialValue);
    }
}
