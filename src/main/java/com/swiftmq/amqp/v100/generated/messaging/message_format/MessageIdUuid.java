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

package com.swiftmq.amqp.v100.generated.messaging.message_format;

import com.swiftmq.amqp.v100.types.AMQPUuid;

import java.util.UUID;

/**
 * @author IIT Software GmbH, Bremen/Germany, (c) 2012, All Rights Reserved
 * @version AMQP Version v100. Generation Date: Wed Apr 18 14:09:32 CEST 2012
 **/

public class MessageIdUuid extends AMQPUuid
        implements MessageIdIF {


    /**
     * Constructs a MessageIdUuid.
     *
     * @param initValue initial value
     */
    public MessageIdUuid(UUID initValue) {
        super(initValue);
    }

    /**
     * Accept method for a MessageId visitor.
     *
     * @param visitor MessageId visitor
     */
    public void accept(MessageIdVisitor visitor) {
        visitor.visit(this);
    }


    public String toString() {
        return "[MessageIdUuid " + super.toString() + "]";
    }
}
