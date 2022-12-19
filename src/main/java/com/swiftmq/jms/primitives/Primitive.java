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

package com.swiftmq.jms.primitives;

public interface Primitive {
    int INT = 0;
    int LONG = 1;
    int DOUBLE = 2;
    int FLOAT = 3;
    int BOOLEAN = 4;
    int CHAR = 5;
    int SHORT = 6;
    int BYTE = 7;
    int BYTES = 8;
    int STRING = 9;

    Object getObject();
}
