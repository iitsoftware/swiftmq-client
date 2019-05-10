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

package com.swiftmq.filetransfer.protocol.v941;

import com.swiftmq.filetransfer.protocol.MessageBasedRequestVisitor;

public interface ProtocolVisitor extends MessageBasedRequestVisitor {
    public void visit(FilePublishRequest request);

    public void visit(FileChunkRequest request);

    public void visit(FileConsumeRequest request);

    public void visit(FileDeleteRequest request);

    public void visit(FileQueryRequest request);

    public void visit(SessionCloseRequest request);

    public void visit(FileQueryPropsRequest request);
}
