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
 * SMQP-Protocol Version 600, Class: CreateSubscriberRequest
 * Automatically generated, don't change!
 * Generation Date: Thu Feb 09 09:59:46 CET 2006
 * (c) 2006, IIT GmbH, Bremen/Germany, All Rights Reserved
 **/

import com.swiftmq.jms.TopicImpl;
import com.swiftmq.tools.requestreply.Reply;
import com.swiftmq.tools.requestreply.Request;
import com.swiftmq.tools.requestreply.RequestRetryValidator;
import com.swiftmq.tools.requestreply.RequestVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class CreateSubscriberRequest extends Request {
    private TopicImpl topic;
    private String messageSelector;
    private boolean noLocal;
    private boolean autoCommit;

    public CreateSubscriberRequest() {
        super(0, true);
    }

    public CreateSubscriberRequest(int dispatchId) {
        super(dispatchId, true);
    }

    public CreateSubscriberRequest(RequestRetryValidator validator, int dispatchId) {
        super(dispatchId, true, validator);
    }

    public CreateSubscriberRequest(int dispatchId, TopicImpl topic, String messageSelector, boolean noLocal, boolean autoCommit) {
        super(dispatchId, true);
        this.topic = topic;
        this.messageSelector = messageSelector;
        this.noLocal = noLocal;
        this.autoCommit = autoCommit;
    }

    public CreateSubscriberRequest(RequestRetryValidator validator, int dispatchId, TopicImpl topic, String messageSelector, boolean noLocal, boolean autoCommit) {
        super(dispatchId, true, validator);
        this.topic = topic;
        this.messageSelector = messageSelector;
        this.noLocal = noLocal;
        this.autoCommit = autoCommit;
    }

    public TopicImpl getTopic() {
        return topic;
    }

    public void setTopic(TopicImpl topic) {
        this.topic = topic;
    }

    public String getMessageSelector() {
        return messageSelector;
    }

    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }

    public boolean isNoLocal() {
        return noLocal;
    }

    public void setNoLocal(boolean noLocal) {
        this.noLocal = noLocal;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public int getDumpId() {
        return SMQPFactory.DID_CREATESUBSCRIBER_REQ;
    }

    public void writeContent(DataOutput out) throws IOException {
        super.writeContent(out);
        SMQPUtil.write(topic, out);
        if (messageSelector != null) {
            out.writeBoolean(true);
            SMQPUtil.write(messageSelector, out);
        } else
            out.writeBoolean(false);
        SMQPUtil.write(noLocal, out);
        SMQPUtil.write(autoCommit, out);
    }

    public void readContent(DataInput in) throws IOException {
        super.readContent(in);
        topic = SMQPUtil.read(topic, in);
        boolean messageSelector_set = in.readBoolean();
        if (messageSelector_set)
            messageSelector = SMQPUtil.read(messageSelector, in);
        noLocal = SMQPUtil.read(noLocal, in);
        autoCommit = SMQPUtil.read(autoCommit, in);
    }

    protected Reply createReplyInstance() {
        return new CreateSubscriberReply();
    }

    public void accept(RequestVisitor visitor) {
        ((SMQPVisitor) visitor).visit(this);
    }

    public String toString() {
        StringBuffer _b = new StringBuffer("[v600/CreateSubscriberRequest, ");
        _b.append(super.toString());
        _b.append(", ");
        _b.append("topic=");
        _b.append(topic);
        _b.append(", ");
        _b.append("messageSelector=");
        _b.append(messageSelector);
        _b.append(", ");
        _b.append("noLocal=");
        _b.append(noLocal);
        _b.append(", ");
        _b.append("autoCommit=");
        _b.append(autoCommit);
        _b.append("]");
        return _b.toString();
    }
}
