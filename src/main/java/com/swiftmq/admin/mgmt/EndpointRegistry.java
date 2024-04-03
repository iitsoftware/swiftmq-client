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

package com.swiftmq.admin.mgmt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EndpointRegistry {
    private final Map<String, Endpoint> endpoints = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    public EndpointRegistry() {
    }

    public void put(String routerName, Endpoint endpoint) throws EndpointRegistryClosedException {
        if (closed)
            throw new EndpointRegistryClosedException("EndpointRegistry already closed!");
        endpoints.put(routerName, endpoint);
    }

    public Endpoint get(String routerName) {
        return (Endpoint) endpoints.get(routerName);
    }

    public Endpoint remove(String routerName) {
        return (Endpoint) endpoints.remove(routerName);
    }

    public void close() {
        for (Map.Entry<String, Endpoint> o : endpoints.entrySet()) {
            Endpoint endpoint = o.getValue();
            endpoint.close();
        }
        endpoints.clear();
        closed = true;
    }
}
