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
 * SMQP-Protocol Version 600, Class: AuthResponseRequest
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

public class AuthResponseRequest extends Request {
    private byte[] response;

    public AuthResponseRequest() {
        super(0, true);
    }

    public AuthResponseRequest(int dispatchId) {
        super(dispatchId, true);
    }

    public AuthResponseRequest(RequestRetryValidator validator, int dispatchId) {
        super(dispatchId, true, validator);
    }

    public AuthResponseRequest(int dispatchId, byte[] response) {
        super(dispatchId, true);
        this.response = response;
    }

    public AuthResponseRequest(RequestRetryValidator validator, int dispatchId, byte[] response) {
        super(dispatchId, true, validator);
        this.response = response;
    }

    public byte[] getResponse() {
        return response;
    }

    public void setResponse(byte[] response) {
        this.response = response;
    }

    public int getDumpId() {
        return SMQPFactory.DID_AUTHRESPONSE_REQ;
    }

    public void writeContent(DataOutput out) throws IOException {
        super.writeContent(out);
        SMQPUtil.write(response, out);
    }

    public void readContent(DataInput in) throws IOException {
        super.readContent(in);
        response = SMQPUtil.read(response, in);
    }

    protected Reply createReplyInstance() {
        return new AuthResponseReply();
    }

    public void accept(RequestVisitor visitor) {
        ((SMQPVisitor) visitor).visit(this);
    }

    public String toString() {
        StringBuffer _b = new StringBuffer("[v600/AuthResponseRequest, ");
        _b.append(super.toString());
        _b.append(", ");
        _b.append("response=");
        _b.append(response);
        _b.append("]");
        return _b.toString();
    }
}
