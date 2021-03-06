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
 * SMQP-Protocol Version 750, Class: DeleteDurableRequest
 * Automatically generated, don't change!
 * Generation Date: Tue Apr 21 10:39:21 CEST 2009
 * (c) 2009, IIT GmbH, Bremen/Germany, All Rights Reserved
 **/

import com.swiftmq.tools.requestreply.Reply;
import com.swiftmq.tools.requestreply.Request;
import com.swiftmq.tools.requestreply.RequestRetryValidator;
import com.swiftmq.tools.requestreply.RequestVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DeleteDurableRequest extends Request {
    private String durableName;

    public DeleteDurableRequest() {
        super(0, true);
    }

    public DeleteDurableRequest(int dispatchId) {
        super(dispatchId, true);
    }

    public DeleteDurableRequest(RequestRetryValidator validator, int dispatchId) {
        super(dispatchId, true, validator);
    }

    public DeleteDurableRequest(int dispatchId, String durableName) {
        super(dispatchId, true);
        this.durableName = durableName;
    }

    public DeleteDurableRequest(RequestRetryValidator validator, int dispatchId, String durableName) {
        super(dispatchId, true, validator);
        this.durableName = durableName;
    }

    public String getDurableName() {
        return durableName;
    }

    public void setDurableName(String durableName) {
        this.durableName = durableName;
    }

    public int getDumpId() {
        return SMQPFactory.DID_DELETEDURABLE_REQ;
    }


    public void writeContent(DataOutput out) throws IOException {
        super.writeContent(out);
        SMQPUtil.write(durableName, out);
    }

    public void readContent(DataInput in) throws IOException {
        super.readContent(in);
        durableName = SMQPUtil.read(durableName, in);
    }

    protected Reply createReplyInstance() {
        return new DeleteDurableReply();
    }

    public void accept(RequestVisitor visitor) {
        ((SMQPVisitor) visitor).visit(this);
    }

    public String toString() {
        StringBuffer _b = new StringBuffer("[v750/DeleteDurableRequest, ");
        _b.append(super.toString());
        _b.append(", ");
        _b.append("durableName=");
        _b.append(durableName);
        _b.append("]");
        return _b.toString();
    }
}
