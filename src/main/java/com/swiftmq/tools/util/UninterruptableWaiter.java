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

package com.swiftmq.tools.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class UninterruptableWaiter {
    private final Lock lock;
    private final Condition condition;
    private boolean signalled = false;

    public UninterruptableWaiter(Lock lock) {
        this.lock = lock;
        this.condition = lock.newCondition();
    }

    public void doWait() {
        lock.lock();
        try {
            boolean wasInterrupted = Thread.interrupted();
            while (!signalled) {
                try {
                    condition.await();
                    if (signalled) {
                        break; // Exit loop if signalled
                    }
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                    // Continue waiting - do not exit loop
                }
            }
            if (wasInterrupted) {
                Thread.currentThread().interrupt(); // Restore interruption status
            }
        } finally {
            signalled = false; // Reset signal flag
            lock.unlock();
        }
    }

    public void doWait(long timeout) {
        if (timeout == 0) {
            doWait();
            return;
        }
        lock.lock();
        try {
            boolean wasInterrupted = Thread.interrupted();
            long nanos = TimeUnit.MILLISECONDS.toNanos(timeout);
            while (!signalled && nanos > 0L) {
                try {
                    nanos = condition.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                    // Continue waiting - do not exit loop
                }
            }
            if (wasInterrupted) {
                Thread.currentThread().interrupt(); // Restore interruption status
            }
        } finally {
            signalled = false; // Reset signal flag
            lock.unlock();
        }
    }

    public void signal() {
        lock.lock();
        try {
            signalled = true;
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void signalAll() {
        lock.lock();
        try {
            signalled = true;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
