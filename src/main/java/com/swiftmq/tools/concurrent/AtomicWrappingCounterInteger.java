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

public class AtomicWrappingCounterInteger {
    private final AtomicReference<Integer> counter;
    private final int initialValue;

    public AtomicWrappingCounterInteger(int initialValue) {
        this.initialValue = initialValue;
        this.counter = new AtomicReference<>(initialValue);
    }

    public int get() {
        return counter.get();
    }

    public int getAndIncrement() {
        return counter.getAndUpdate(current -> (current == Integer.MAX_VALUE) ? initialValue : current + 1);
    }

    public void reset() {
        counter.set(initialValue);
    }
}