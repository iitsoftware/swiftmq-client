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

package com.swiftmq.swiftlet.net;

import com.swiftmq.net.protocol.ProtocolInputHandler;
import com.swiftmq.net.protocol.ProtocolOutputHandler;
import com.swiftmq.swiftlet.Swiftlet;
import com.swiftmq.swiftlet.net.event.ConnectionListener;
import com.swiftmq.tools.sql.LikeComparator;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A ListenerMetaData object describes a TCP listener.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2002, All Rights Reserved
 */
public class ListenerMetaData extends ConnectionMetaData {
    InetAddress bindAddress;
    int port;
    Set<String> hostAccessList = ConcurrentHashMap.newKeySet();

    public ListenerMetaData(InetAddress bindAddress, int port, Swiftlet swiftlet, long keepAliveInterval, String socketFactoryClass, ConnectionListener connectionListener,
                            int inputBufferSize, int inputExtendSize, int outputBufferSize, int outputExtendSize) {
        super(swiftlet, keepAliveInterval, socketFactoryClass, connectionListener,
                inputBufferSize, inputExtendSize, outputBufferSize, outputExtendSize, true);
        this.bindAddress = bindAddress;
        this.port = port;
    }

    public ListenerMetaData(InetAddress bindAddress, int port, Swiftlet swiftlet, long keepAliveInterval, String socketFactoryClass, ConnectionListener connectionListener,
                            int inputBufferSize, int inputExtendSize, int outputBufferSize, int outputExtendSize, boolean useTcpNoDelay) {
        super(swiftlet, keepAliveInterval, socketFactoryClass, connectionListener,
                inputBufferSize, inputExtendSize, outputBufferSize, outputExtendSize, useTcpNoDelay);
        this.bindAddress = bindAddress;
        this.port = port;
    }

    public ListenerMetaData(InetAddress bindAddress, int port, Swiftlet swiftlet, long keepAliveInterval, String socketFactoryClass, ConnectionListener connectionListener,
                            int inputBufferSize, int inputExtendSize, int outputBufferSize, int outputExtendSize,
                            ProtocolInputHandler protocolInputHandler, ProtocolOutputHandler protocolOutputHandler) {
        super(swiftlet, keepAliveInterval, socketFactoryClass, connectionListener,
                inputBufferSize, inputExtendSize, outputBufferSize, outputExtendSize, true, protocolInputHandler, protocolOutputHandler);
        this.bindAddress = bindAddress;
        this.port = port;
    }

    public ListenerMetaData(InetAddress bindAddress, int port, Swiftlet swiftlet, long keepAliveInterval, String socketFactoryClass, ConnectionListener connectionListener,
                            int inputBufferSize, int inputExtendSize, int outputBufferSize, int outputExtendSize, boolean useTcpNoDelay,
                            ProtocolInputHandler protocolInputHandler, ProtocolOutputHandler protocolOutputHandler) {
        super(swiftlet, keepAliveInterval, socketFactoryClass, connectionListener,
                inputBufferSize, inputExtendSize, outputBufferSize, outputExtendSize, useTcpNoDelay, protocolInputHandler, protocolOutputHandler);
        this.bindAddress = bindAddress;
        this.port = port;
    }


    public InetAddress getBindAddress() {
        return (bindAddress);
    }

    public int getPort() {
        return (port);
    }


    public void addToHostAccessList(String predicate) {
        hostAccessList.add(predicate);
    }

    public void removeFromHostAccessList(String predicate) {
        hostAccessList.remove(predicate);
    }

    public boolean isConnectionAllowed(String hostname) {
        if (hostAccessList.isEmpty())
            return true;
        else {
            for (String allowedHost : hostAccessList) {
                if (LikeComparator.compare(hostname, allowedHost, '\\')) {
                    return true;
                }
            }
            return false;
        }
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("[ListenerMetaData, ");
        b.append(super.toString());
        b.append(", bindAddress=");
        b.append(bindAddress);
        b.append(", port=");
        b.append(port);
        b.append(", hostAccessList=");
        b.append(hostAccessList);
        b.append("]");
        return b.toString();
    }
}

