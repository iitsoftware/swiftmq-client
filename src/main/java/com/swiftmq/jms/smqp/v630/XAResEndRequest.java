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

package com.swiftmq.jms.smqp.v630;

/**
 * SMQP-Protocol Version 630, Class: XAResEndRequest
 * Automatically generated, don't change!
 * Generation Date: Thu Aug 30 17:17:54 CEST 2007
 * (c) 2007, IIT GmbH, Bremen/Germany, All Rights Reserved
 **/

import com.swiftmq.jms.XidImpl;
import com.swiftmq.tools.requestreply.Reply;
import com.swiftmq.tools.requestreply.Request;
import com.swiftmq.tools.requestreply.RequestRetryValidator;
import com.swiftmq.tools.requestreply.RequestVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public class XAResEndRequest extends Request {
    private XidImpl xid;
    private int flags;
    private boolean retry;
    private List messages;
    private List recoverRequestList;

    public XAResEndRequest() {
        super(0, true);
    }

    public XAResEndRequest(int dispatchId) {
        super(dispatchId, true);
    }

    public XAResEndRequest(RequestRetryValidator validator, int dispatchId) {
        super(dispatchId, true, validator);
    }

    public XAResEndRequest(int dispatchId, XidImpl xid, int flags, boolean retry, List messages, List recoverRequestList) {
        super(dispatchId, true);
        this.xid = xid;
        this.flags = flags;
        this.retry = retry;
        this.messages = messages;
        this.recoverRequestList = recoverRequestList;
    }

    public XAResEndRequest(RequestRetryValidator validator, int dispatchId, XidImpl xid, int flags, boolean retry, List messages, List recoverRequestList) {
        super(dispatchId, true, validator);
        this.xid = xid;
        this.flags = flags;
        this.retry = retry;
        this.messages = messages;
        this.recoverRequestList = recoverRequestList;
    }

    public XidImpl getXid() {
        return xid;
    }

    public void setXid(XidImpl xid) {
        this.xid = xid;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }

    public List getMessages() {
        return messages;
    }

    public void setMessages(List messages) {
        this.messages = messages;
    }

    public List getRecoverRequestList() {
        return recoverRequestList;
    }

    public void setRecoverRequestList(List recoverRequestList) {
        this.recoverRequestList = recoverRequestList;
    }

    public int getDumpId() {
        return SMQPFactory.DID_XARESEND_REQ;
    }

    public void writeContent(DataOutput out) throws IOException {
        super.writeContent(out);
        SMQPUtil.write(xid, out);
        SMQPUtil.write(flags, out);
        SMQPUtil.write(retry, out);
        if (messages != null) {
            out.writeBoolean(true);
            SMQPUtil.writeMessageList(messages, out);
        } else
            out.writeBoolean(false);
        if (recoverRequestList != null) {
            out.writeBoolean(true);
            SMQPUtil.writeRequest(recoverRequestList, out);
        } else
            out.writeBoolean(false);
    }

    public void readContent(DataInput in) throws IOException {
        super.readContent(in);
        xid = SMQPUtil.read(xid, in);
        flags = SMQPUtil.read(flags, in);
        retry = SMQPUtil.read(retry, in);
        boolean messages_set = in.readBoolean();
        if (messages_set)
            messages = SMQPUtil.readMessageList(messages, in);
        boolean recoverRequestList_set = in.readBoolean();
        if (recoverRequestList_set)
            recoverRequestList = SMQPUtil.readRequest(recoverRequestList, in);
    }

    protected Reply createReplyInstance() {
        return new XAResEndReply();
    }

    public void accept(RequestVisitor visitor) {
        ((SMQPVisitor) visitor).visit(this);
    }

    public String toString() {
        StringBuffer _b = new StringBuffer("[v630/XAResEndRequest, ");
        _b.append(super.toString());
        _b.append(", ");
        _b.append("xid=");
        _b.append(xid);
        _b.append(", ");
        _b.append("flags=");
        _b.append(flags);
        _b.append(", ");
        _b.append("retry=");
        _b.append(retry);
        _b.append(", ");
        _b.append("messages=");
        _b.append(messages);
        _b.append(", ");
        _b.append("recoverRequestList=");
        _b.append(recoverRequestList);
        _b.append("]");
        return _b.toString();
    }
}
