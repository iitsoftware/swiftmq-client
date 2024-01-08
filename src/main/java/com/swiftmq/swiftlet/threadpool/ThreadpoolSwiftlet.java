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

package com.swiftmq.swiftlet.threadpool;

import com.swiftmq.swiftlet.Swiftlet;

import java.util.concurrent.CompletableFuture;

/**
 * The ThreadpoolSwiftlet manages threads of a SwiftMQ router.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2005, All Rights Reserved
 */
public abstract class ThreadpoolSwiftlet extends Swiftlet {

    /**
     * Executes a Runnable in a separate thread.
     *
     * @param r Runnable
     * @return Future to track completion
     */
    public abstract CompletableFuture<?> runAsync(Runnable r);

    /**
     * Create a new event loop.
     *
     * @param id     id of the client
     * @param processor The processor that executes the tasks
     * @return A new Event Loop
     */
    public abstract EventLoop createEventLoop(String id, EventProcessor processor);

    /**
     * Freeze all event loops and async thread pools.
     *
     * @return A future
     */
    public abstract CompletableFuture<Void> freeze();

    /**
     * Unfreeze all event loops and async thread pools.
     *
     * @return A future
     */
    public abstract CompletableFuture<Void> unfreeze();

}

