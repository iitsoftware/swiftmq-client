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

import java.util.concurrent.atomic.AtomicReference;

public class RouterConfiguration {
    private static final AtomicReference<RouterConfigInstance> instance = new AtomicReference<>();

    public static RouterConfigInstance Singleton() {
        // Use compareAndSet to atomically initialize the instance
        instance.compareAndSet(null, new RouterConfigInstance());
        return instance.get();
    }

    public static void removeInstance() {
        instance.updateAndGet(currentInstance -> {
            if (currentInstance != null) {
                currentInstance.clearConfigurations();
                return null;
            }
            return currentInstance;
        });
    }
}

