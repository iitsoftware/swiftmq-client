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

package com.swiftmq.jms.smqp.v500;

import com.swiftmq.jms.DestinationFactory;
import com.swiftmq.jms.TopicImpl;
import com.swiftmq.tools.requestreply.Reply;
import com.swiftmq.tools.requestreply.Request;
import com.swiftmq.tools.requestreply.RequestVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Andreas Mueller, IIT GmbH
 * @version 1.0
 */
public class CreateSubscriberRequest extends Request {
    TopicImpl topic = null;
    String messageSelector = null;
    boolean noLocal = false;

    /**
     * @param topic
     * @param messageSelector
     * @param noLocal
     * @param dispatchId
     * @SBGen Constructor assigns topic, messageSelector
     */
    public CreateSubscriberRequest(int dispatchId, TopicImpl topic,
                                   String messageSelector, boolean noLocal) {
        super(dispatchId, true);

        // SBgen: Assign variables
        this.topic = topic;
        this.messageSelector = messageSelector;
        this.noLocal = noLocal;
        // SBgen: End assign
    }

    /**
     * Returns a unique dump id for this object.
     *
     * @return unique dump id
     */
    public int getDumpId() {
        return SMQPFactory.DID_CREATE_SUBSCRIBER_REQ;
    }

    /**
     * Write the content of this object to the stream.
     *
     * @param out output stream
     * @throws IOException if an error occurs
     */
    public void writeContent(DataOutput out) throws IOException {
        super.writeContent(out);

        out.writeBoolean(noLocal);

        if (topic == null) {
            out.writeByte(0);
        } else {
            out.writeByte(1);
            DestinationFactory.dumpDestination(topic, out);
        }

        if (messageSelector == null) {
            out.writeByte(0);
        } else {
            out.writeByte(1);
            out.writeUTF(messageSelector);
        }
    }

    /**
     * Read the content of this object from the stream.
     *
     * @param in input stream
     * @throws IOException if an error occurs
     */
    public void readContent(DataInput in) throws IOException {
        super.readContent(in);

        noLocal = in.readBoolean();

        byte set = in.readByte();

        if (set == 0) {
            topic = null;
        } else {
            topic = (TopicImpl) DestinationFactory.createDestination(in);
        }

        set = in.readByte();

        if (set == 0) {
            messageSelector = null;
        } else {
            messageSelector = in.readUTF();
        }
    }

    /**
     * @return
     */
    protected Reply createReplyInstance() {
        return new CreateSubscriberReply();
    }

    /**
     * @return
     * @SBGen Method get topic
     */
    public TopicImpl getTopic() {

        // SBgen: Get variable
        return (topic);
    }

    /**
     * @param topic
     * @SBGen Method set topic
     */
    public void setTopic(TopicImpl topic) {

        // SBgen: Assign variable
        this.topic = topic;
    }

    /**
     * @return
     * @SBGen Method get messageSelector
     */
    public String getMessageSelector() {

        // SBgen: Get variable
        return (messageSelector);
    }

    /**
     * @param messageSelector
     * @SBGen Method set messageSelector
     */
    public void setMessageSelector(String messageSelector) {

        // SBgen: Assign variable
        this.messageSelector = messageSelector;
    }

    /**
     * @return
     * @SBGen Method get noLocal
     */
    public boolean isNoLocal() {
        // SBgen: Get variable
        return (noLocal);
    }

    /**
     * @param noLocal
     * @SBGen Method set noLocal
     */
    public void setNoLocal(boolean noLocal) {

        // SBgen: Assign variable
        this.noLocal = noLocal;
    }

    public void accept(RequestVisitor visitor) {
        ((SMQPVisitor) visitor).visitCreateSubscriberRequest(this);
    }

    /**
     * Method declaration
     *
     * @return
     * @see
     */
    public String toString() {
        return "[CreateSubscriberRequest " + super.toString() + " topic=" + topic
                + " messageSelector=" + messageSelector + "]";
    }

}



