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

package com.swiftmq.jms.v750;

import com.swiftmq.jms.XidImpl;
import com.swiftmq.tools.requestreply.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XARecoverRegistry {
    Map<XidImpl, List> xidMap = new ConcurrentHashMap<>();


    private XARecoverRegistry() {
    }

    public static XARecoverRegistry getInstance() {
        return InstanceHolder.instance;
    }

    public void addRequest(XidImpl xid, Request request) {
        List list = (List) xidMap.get(xid);
        if (list == null) {
            list = new ArrayList();
            xidMap.put(xid, list);
        }
        list.add(request);
    }

    public List getRequestList(XidImpl xid) {
        ArrayList l = (ArrayList) xidMap.get(xid);
        if (l != null)
            return (List) l.clone();
        return null;
    }

    public void clear(XidImpl xid) {
        xidMap.remove(xid);
    }

    public void clear() {
        xidMap.clear();
    }

    private static class InstanceHolder {
        public static XARecoverRegistry instance = new XARecoverRegistry();
    }

}
