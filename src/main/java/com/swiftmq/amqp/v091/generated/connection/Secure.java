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

package com.swiftmq.amqp.v091.generated.connection;

/**
 * AMQP-Protocol Version 091
 * Automatically generated, don't change!
 * Generation Date: Thu Apr 12 12:18:24 CEST 2012
 * (c) 2012, IIT Software GmbH, Bremen/Germany
 * All Rights Reserved
 **/

import com.swiftmq.amqp.v091.io.BitSupportDataInput;
import com.swiftmq.amqp.v091.io.BitSupportDataOutput;
import com.swiftmq.amqp.v091.types.Coder;

import java.io.IOException;

public class Secure extends ConnectionMethod {
    byte[] challenge;

    public Secure() {
        _classId = 10;
        _methodId = 20;
    }

    public void accept(ConnectionMethodVisitor visitor) {
        visitor.visit(this);
    }

    public byte[] getChallenge() {
        return challenge;
    }

    public void setChallenge(byte[] challenge) {
        this.challenge = challenge;
    }

    protected void readBody(BitSupportDataInput in) throws IOException {
        challenge = Coder.readLongString(in);
    }

    protected void writeBody(BitSupportDataOutput out) throws IOException {
        Coder.writeLongString(challenge, out);
        out.bitFlush();
    }

    private String getDisplayString() {
        boolean _first = true;
        StringBuffer b = new StringBuffer(" ");
        if (!_first)
            b.append(", ");
        else
            _first = false;
        b.append("challenge=");
        if (challenge != null)
            b.append(new String(challenge));
        else
            b.append("null");
        return b.toString();
    }

    public String toString() {
        return "[Secure " + super.toString() + getDisplayString() + "]";
    }
}
