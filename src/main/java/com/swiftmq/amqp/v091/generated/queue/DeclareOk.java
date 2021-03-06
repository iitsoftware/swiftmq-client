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

package com.swiftmq.amqp.v091.generated.queue;

/**
 * AMQP-Protocol Version 091
 * Automatically generated, don't change!
 * Generation Date: Thu Apr 12 12:18:24 CEST 2012
 * (c) 2012, IIT Software GmbH, Bremen/Germany
 * All Rights Reserved
 **/

import com.swiftmq.amqp.v091.io.BitSupportDataInput;
import com.swiftmq.amqp.v091.io.BitSupportDataOutput;
import com.swiftmq.amqp.v091.types.Coder;

import java.io.IOException;

public class DeclareOk extends QueueMethod {
    String queue;
    int messageCount;
    int consumerCount;

    public DeclareOk() {
        _classId = 50;
        _methodId = 11;
    }

    public void accept(QueueMethodVisitor visitor) {
        visitor.visit(this);
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int getConsumerCount() {
        return consumerCount;
    }

    public void setConsumerCount(int consumerCount) {
        this.consumerCount = consumerCount;
    }

    protected void readBody(BitSupportDataInput in) throws IOException {
        queue = Coder.readShortString(in);
        messageCount = Coder.readInt(in);
        consumerCount = Coder.readInt(in);
    }

    protected void writeBody(BitSupportDataOutput out) throws IOException {
        Coder.writeShortString(queue, out);
        Coder.writeInt(messageCount, out);
        Coder.writeInt(consumerCount, out);
        out.bitFlush();
    }

    private String getDisplayString() {
        boolean _first = true;
        StringBuffer b = new StringBuffer(" ");
        if (!_first)
            b.append(", ");
        else
            _first = false;
        b.append("queue=");
        b.append(queue);
        if (!_first)
            b.append(", ");
        else
            _first = false;
        b.append("messageCount=");
        b.append(messageCount);
        if (!_first)
            b.append(", ");
        else
            _first = false;
        b.append("consumerCount=");
        b.append(consumerCount);
        return b.toString();
    }

    public String toString() {
        return "[DeclareOk " + super.toString() + getDisplayString() + "]";
    }
}
