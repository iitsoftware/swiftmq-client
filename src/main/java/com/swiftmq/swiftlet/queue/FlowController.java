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

package com.swiftmq.swiftlet.queue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for flow controllers. A flow controller computes a delay,
 * dependent on throughput, queue backlog, and transaction size.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2002, All Rights Reserved
 */
public abstract class FlowController {
    protected final AtomicInteger receiverCount = new AtomicInteger();
    protected final AtomicInteger queueSize = new AtomicInteger();
    protected final AtomicLong lastDelay = new AtomicLong();
    protected final AtomicLong sentCount = new AtomicLong();
    protected final AtomicLong sentCountCalls = new AtomicLong();
    protected final AtomicLong receiveCount = new AtomicLong();
    protected final AtomicLong receiveCountCalls = new AtomicLong();
    protected final AtomicLong timestamp = new AtomicLong();


    /**
     * Returns the FC start queue size
     *
     * @return start queue size
     */
    public int getStartQueueSize() {
        return 0;
    }

    /**
     * Sets the queue size (message count).
     *
     * @param queueSize queue size.
     */
    public void setQueueSize(int queueSize) {
        this.queueSize.set(queueSize);
    }


    /**
     * Sets the receiver count.
     *
     * @param count receiver count.
     */
    public void setReceiverCount(int count) {
        if (count == 0) {
            sentCount.set(0);
            sentCountCalls.set(0);
            receiveCount.set(0);
            receiveCountCalls.set(0);
            timestamp.set(0);
        } else if (receiverCount.get() == 0 && count > 0) {
            timestamp.set(System.currentTimeMillis());
            sentCount.set(queueSize.get());
            sentCountCalls.set(queueSize.get());
        }
        receiverCount.set(count);
    }


    /**
     * Sets the receive message count.
     *
     * @param count receive message count.
     */
    public void setReceiveMessageCount(int count) {
        if (timestamp.get() != 0) {
            receiveCount.addAndGet(count);
            receiveCountCalls.getAndIncrement();
        }
    }


    /**
     * Sets the sent message count.
     *
     * @param count sent message count.
     */
    public void setSentMessageCount(int count) {
        if (timestamp.get() != 0) {
            sentCount.addAndGet(count);
            sentCountCalls.getAndIncrement();
        }
    }


    /**
     * Returns the last computed fc delay.
     *
     * @return delay.
     */
    public long getLastDelay() {
        return lastDelay.get();
    }


    /**
     * Computes and returns a new delay.
     *
     * @return delay.
     */
    public abstract long getNewDelay();

    public String toString() {
        StringBuffer b = new StringBuffer("[FlowController, ");
        b.append("timestamp=");
        b.append(timestamp.get());
        b.append(", receiveCount=");
        b.append(receiveCount.get());
        b.append(", receiveCountCalls=");
        b.append(receiveCountCalls.get());
        b.append(", sentCount=");
        b.append(sentCount.get());
        b.append(", sentCountCalls=");
        b.append(sentCountCalls.get());
        b.append("]");
        return b.toString();
    }
}

