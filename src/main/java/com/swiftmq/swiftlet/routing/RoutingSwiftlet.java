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

package com.swiftmq.swiftlet.routing;

import com.swiftmq.swiftlet.Swiftlet;
import com.swiftmq.swiftlet.routing.event.RoutingEvent;
import com.swiftmq.swiftlet.routing.event.RoutingListener;
import com.swiftmq.tools.collection.ConcurrentList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The RoutingSwiftlet manages connections as well as message routing
 * to remote destinations.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2002, All Rights Reserved
 */
public abstract class RoutingSwiftlet extends Swiftlet {
    private final Map<String, Route> routingTable = new ConcurrentHashMap<>();
    private final List<RoutingListener> listeners = new ConcurrentList<>(new ArrayList<>());

    public Route getRoute(String destination) {
        return routingTable.get(destination);
    }

    public Route[] getRoutes() {
        Route[] routes = new Route[routingTable.size()];
        int i = 0;
        for (Route route : routingTable.values()) {
            routes[i++] = route;
        }
        return routes;
    }

    protected void addRoute(Route route) {
        routingTable.put(route.getDestination(), route);
        fireRoutingEvent("destinationAdded", new RoutingEvent(this, route.getDestination()));
    }

    protected void removeRoute(Route route) {
        routingTable.remove(route.getDestination());
        fireRoutingEvent("destinationRemoved", new RoutingEvent(this, route.getDestination()));
    }

    protected void removeAllRoutes() {
        for (Route route : routingTable.values()) {
            removeRoute(route);
        }
    }

    public void addRoutingListener(RoutingListener l) {
        listeners.add(l);
    }

    public void removeRoutingListener(RoutingListener l) {
        listeners.remove(l);
    }

    protected void removeAllRoutingListeners() {
        listeners.clear();
    }

    public void fireRoutingEvent(String method, RoutingEvent evt) {
        for (RoutingListener l : listeners) {
            switch (method) {
                case "destinationAdded":
                    l.destinationAdded(evt);
                    break;
                case "destinationRemoved":
                    l.destinationRemoved(evt);
                    break;
                case "destinationActivated":
                    l.destinationActivated(evt);
                    break;
                case "destinationDeactivated":
                    l.destinationDeactivated(evt);
                    break;
                // other cases as needed
            }
        }
    }
}