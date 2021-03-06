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

package com.swiftmq.jms.smqp.v600;

/**
 * SMQP-Protocol Version 600, Class: CloseConsumerRequest
 * Automatically generated, don't change!
 * Generation Date: Thu Feb 09 09:59:46 CET 2006
 * (c) 2006, IIT GmbH, Bremen/Germany, All Rights Reserved
 **/

import com.swiftmq.tools.requestreply.Reply;
import com.swiftmq.tools.requestreply.Request;
import com.swiftmq.tools.requestreply.RequestRetryValidator;
import com.swiftmq.tools.requestreply.RequestVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class CloseConsumerRequest extends Request {
    private int sessionDispatchId;
    private int queueConsumerId;
    private String consumerException;

    public CloseConsumerRequest() {
        super(0, true);
    }

    public CloseConsumerRequest(int dispatchId) {
        super(dispatchId, true);
    }

    public CloseConsumerRequest(RequestRetryValidator validator, int dispatchId) {
        super(dispatchId, true, validator);
    }

    public CloseConsumerRequest(int dispatchId, int sessionDispatchId, int queueConsumerId, String consumerException) {
        super(dispatchId, true);
        this.sessionDispatchId = sessionDispatchId;
        this.queueConsumerId = queueConsumerId;
        this.consumerException = consumerException;
    }

    public CloseConsumerRequest(RequestRetryValidator validator, int dispatchId, int sessionDispatchId, int queueConsumerId, String consumerException) {
        super(dispatchId, true, validator);
        this.sessionDispatchId = sessionDispatchId;
        this.queueConsumerId = queueConsumerId;
        this.consumerException = consumerException;
    }

    public int getSessionDispatchId() {
        return sessionDispatchId;
    }

    public void setSessionDispatchId(int sessionDispatchId) {
        this.sessionDispatchId = sessionDispatchId;
    }

    public int getQueueConsumerId() {
        return queueConsumerId;
    }

    public void setQueueConsumerId(int queueConsumerId) {
        this.queueConsumerId = queueConsumerId;
    }

    public String getConsumerException() {
        return consumerException;
    }

    public void setConsumerException(String consumerException) {
        this.consumerException = consumerException;
    }

    public int getDumpId() {
        return SMQPFactory.DID_CLOSECONSUMER_REQ;
    }

    public void writeContent(DataOutput out) throws IOException {
        super.writeContent(out);
        SMQPUtil.write(sessionDispatchId, out);
        SMQPUtil.write(queueConsumerId, out);
        if (consumerException != null) {
            out.writeBoolean(true);
            SMQPUtil.write(consumerException, out);
        } else
            out.writeBoolean(false);
    }

    public void readContent(DataInput in) throws IOException {
        super.readContent(in);
        sessionDispatchId = SMQPUtil.read(sessionDispatchId, in);
        queueConsumerId = SMQPUtil.read(queueConsumerId, in);
        boolean consumerException_set = in.readBoolean();
        if (consumerException_set)
            consumerException = SMQPUtil.read(consumerException, in);
    }

    protected Reply createReplyInstance() {
        return new CloseConsumerReply();
    }

    public void accept(RequestVisitor visitor) {
        ((SMQPVisitor) visitor).visit(this);
    }

    public String toString() {
        StringBuffer _b = new StringBuffer("[v600/CloseConsumerRequest, ");
        _b.append(super.toString());
        _b.append(", ");
        _b.append("sessionDispatchId=");
        _b.append(sessionDispatchId);
        _b.append(", ");
        _b.append("queueConsumerId=");
        _b.append(queueConsumerId);
        _b.append(", ");
        _b.append("consumerException=");
        _b.append(consumerException);
        _b.append("]");
        return _b.toString();
    }
}
