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

/*--- formatted by Jindent 2.1, (www.c-lab.de/~jindent) ---*/

package com.swiftmq.jms.smqp.v400;

import com.swiftmq.tools.requestreply.Reply;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Andreas Mueller, IIT GmbH
 * @version 1.0
 */
public class ProduceMessageReply extends Reply {
    long delay = 0;

    /**
     * Returns a unique dump id for this object.
     *
     * @return unique dump id
     */
    public int getDumpId() {
        return SMQPFactory.DID_PRODUCE_MESSAGE_REP;
    }

    /**
     * Write the content of this object to the stream.
     *
     * @param out output stream
     * @throws IOException if an error occurs
     */
    public void writeContent(DataOutput out) throws IOException {
        super.writeContent(out);
        out.writeLong(delay);
    }

    /**
     * Read the content of this object from the stream.
     *
     * @param in input stream
     * @throws IOException if an error occurs
     */
    public void readContent(DataInput in) throws IOException {
        super.readContent(in);
        delay = in.readLong();
    }

    /**
     * @return
     * @SBGen Method get delay
     */
    public long getDelay() {

        // SBgen: Get variable
        return (delay);
    }

    /**
     * @param delay
     * @SBGen Method set delay
     */
    public void setDelay(long delay) {

        // SBgen: Assign variable
        this.delay = delay;
    }

    /**
     * Method declaration
     *
     * @return
     * @see
     */
    public String toString() {
        return "[ProduceMessageReply " + super.toString() + ", delay=" + delay + "]";
    }

}



