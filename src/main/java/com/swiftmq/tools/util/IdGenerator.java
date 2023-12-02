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

import com.swiftmq.tools.concurrent.AtomicWrappingCounterLong;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {
    final AtomicLong randomId = new AtomicLong();
    final AtomicWrappingCounterLong id = new AtomicWrappingCounterLong(0);

    private IdGenerator() {
        Random random = new Random();
        randomId.set(random.nextLong());
    }

    public static IdGenerator getInstance() {
        return InstanceHolder.instance;
    }

    public String nextId(char delimiter) {
        StringBuffer b = new StringBuffer();
        b.append(randomId.get());
        b.append(delimiter);
        b.append(id.getAndIncrement());
        return b.toString();
    }

    private static class InstanceHolder {
        public static IdGenerator instance = new IdGenerator();
    }
}
