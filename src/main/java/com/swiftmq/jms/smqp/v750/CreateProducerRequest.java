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

package com.swiftmq.jms.smqp.v750;

/**
 * SMQP-Protocol Version 750, Class: CreateProducerRequest
 * Automatically generated, don't change!
 * Generation Date: Tue Apr 21 10:39:21 CEST 2009
 * (c) 2009, IIT GmbH, Bremen/Germany, All Rights Reserved
 **/

import com.swiftmq.jms.QueueImpl;
import com.swiftmq.tools.requestreply.Reply;
import com.swiftmq.tools.requestreply.Request;
import com.swiftmq.tools.requestreply.RequestRetryValidator;
import com.swiftmq.tools.requestreply.RequestVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class CreateProducerRequest extends Request {
    private QueueImpl queue;

    public CreateProducerRequest() {
        super(0, true);
    }

    public CreateProducerRequest(int dispatchId) {
        super(dispatchId, true);
    }

    public CreateProducerRequest(RequestRetryValidator validator, int dispatchId) {
        super(dispatchId, true, validator);
    }

    public CreateProducerRequest(int dispatchId, QueueImpl queue) {
        super(dispatchId, true);
        this.queue = queue;
    }

    public CreateProducerRequest(RequestRetryValidator validator, int dispatchId, QueueImpl queue) {
        super(dispatchId, true, validator);
        this.queue = queue;
    }

    public QueueImpl getQueue() {
        return queue;
    }

    public void setQueue(QueueImpl queue) {
        this.queue = queue;
    }

    public int getDumpId() {
        return SMQPFactory.DID_CREATEPRODUCER_REQ;
    }


    public void writeContent(DataOutput out) throws IOException {
        super.writeContent(out);
        SMQPUtil.write(queue, out);
    }

    public void readContent(DataInput in) throws IOException {
        super.readContent(in);
        queue = SMQPUtil.read(queue, in);
    }

    protected Reply createReplyInstance() {
        return new CreateProducerReply();
    }

    public void accept(RequestVisitor visitor) {
        ((SMQPVisitor) visitor).visit(this);
    }

    public String toString() {
        StringBuffer _b = new StringBuffer("[v750/CreateProducerRequest, ");
        _b.append(super.toString());
        _b.append(", ");
        _b.append("queue=");
        _b.append(queue);
        _b.append("]");
        return _b.toString();
    }
}
