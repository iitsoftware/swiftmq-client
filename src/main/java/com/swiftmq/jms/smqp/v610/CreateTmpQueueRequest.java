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

package com.swiftmq.jms.smqp.v610;

/**
 * SMQP-Protocol Version 610, Class: CreateTmpQueueRequest
 * Automatically generated, don't change!
 * Generation Date: Mon Jul 17 17:50:10 CEST 2006
 * (c) 2006, IIT GmbH, Bremen/Germany, All Rights Reserved
 **/

import com.swiftmq.tools.requestreply.Reply;
import com.swiftmq.tools.requestreply.Request;
import com.swiftmq.tools.requestreply.RequestRetryValidator;
import com.swiftmq.tools.requestreply.RequestVisitor;

public class CreateTmpQueueRequest extends Request {

    public CreateTmpQueueRequest() {
        super(0, true);
    }

    public CreateTmpQueueRequest(int dispatchId) {
        super(dispatchId, true);
    }

    public CreateTmpQueueRequest(RequestRetryValidator validator, int dispatchId) {
        super(dispatchId, true, validator);
    }


    public int getDumpId() {
        return SMQPFactory.DID_CREATETMPQUEUE_REQ;
    }


    protected Reply createReplyInstance() {
        return new CreateTmpQueueReply();
    }

    public void accept(RequestVisitor visitor) {
        ((SMQPVisitor) visitor).visit(this);
    }

    public String toString() {
        StringBuffer _b = new StringBuffer("[v610/CreateTmpQueueRequest, ");
        _b.append(super.toString());
        _b.append("]");
        return _b.toString();
    }
}
