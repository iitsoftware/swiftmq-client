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

package com.swiftmq.mgmt;

import com.swiftmq.tools.dump.Dumpable;
import com.swiftmq.util.SwiftUtilities;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Command object. Commands are
 * registered at a CommandRegistry, attach to Entities and EntityLists, and
 * performed by CLI and SwiftMQ Explorer. The acutal command action is performed
 * through a CommandExecutor, attached to the Command object.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2002, All Rights Reserved
 * @see CommandRegistry
 * @see Entity
 * @see EntityList
 */
public class Command implements Dumpable {
    final AtomicBoolean guiEnabled = new AtomicBoolean(false);
    final AtomicBoolean guiForChild = new AtomicBoolean(false);
    final AtomicReference<String> name = new AtomicReference<String>();
    volatile String[] tokens = new String[0];
    final AtomicReference<String> pattern = new AtomicReference<String>();
    final AtomicReference<String> description = new AtomicReference<String>();
    final AtomicBoolean enabled = new AtomicBoolean(false);
    Entity parent;
    transient CommandExecutor commandExecutor;


    /**
     * Create a new Command.
     *
     * @param name            the name of the command, e.g. "new".
     * @param pattern         a help pattern, displayed from CLI, e.g. "new <name> [<prop> <value> ...]".
     * @param description     a command description, e.g. "New Entity"
     * @param enabled         true/false.
     * @param commandExecutor the executor.
     */
    public Command(String name, String pattern, String description, boolean enabled, CommandExecutor commandExecutor) {
        this(name, pattern, description, enabled, commandExecutor, false, false);
    }


    /**
     * Create a new Command.
     *
     * @param name            the name of the command, e.g. "new".
     * @param pattern         a help pattern, displayed from CLI, e.g. "new <name> [<prop> <value> ...]".
     * @param description     a command description, e.g. "New Entity"
     * @param enabled         true/false.
     * @param commandExecutor the executor.
     * @param guiEnabled      states whether it is a command shown in the SwiftMQ Explorer.
     * @param guiForChild     states whether it is a command to be attached to each child of an Entity instead of the ENtity itself.
     */
    public Command(String name, String pattern, String description, boolean enabled, CommandExecutor commandExecutor, boolean guiEnabled, boolean guiForChild) {
        // SBgen: Assign variables
        this.name.set(name);
        this.pattern.set(pattern);
        this.description.set(description);
        this.enabled.set(enabled);
        this.commandExecutor = commandExecutor;
        this.guiEnabled.set(guiEnabled);
        this.guiForChild.set(guiForChild);
        // SBgen: End assign
        tokens = SwiftUtilities.tokenize(name, " ");
    }

    Command() {
    }

    public int getDumpId() {
        return MgmtFactory.COMMAND;
    }

    private void writeDump(DataOutput out, String s) throws IOException {
        if (s == null)
            out.writeByte(0);
        else {
            out.writeByte(1);
            out.writeUTF(s);
        }
    }

    private String readDump(DataInput in) throws IOException {
        byte set = in.readByte();
        if (set == 1)
            return in.readUTF();
        return null;
    }

    public void writeContent(DataOutput out)
            throws IOException {
        writeDump(out, name.get());
        writeDump(out, pattern.get());
        writeDump(out, description.get());
        out.writeBoolean(enabled.get());
        out.writeBoolean(guiEnabled.get());
        out.writeBoolean(guiForChild.get());
    }

    public void readContent(DataInput in)
            throws IOException {
        name.set(readDump(in));
        pattern.set(readDump(in));
        description.set(readDump(in));
        enabled.set(in.readBoolean());
        guiEnabled.set(in.readBoolean());
        guiForChild.set(in.readBoolean());
        tokens = SwiftUtilities.tokenize(name.get(), " ");
    }

    /**
     * Returns the pattern.
     *
     * @return pattern.
     */
    public String getPattern() {
        // SBgen: Get variable
        return (pattern.get());
    }


    /**
     * Returns the name.
     *
     * @return name.
     */
    public String getName() {
        // SBgen: Get variable
        return (name.get());
    }


    /**
     * Returns the tokenized name.
     * For example, "show template" returns String[]{"show","template"}
     *
     * @return tokenized name.
     */
    public String[] getTokens() {
        // SBgen: Get variable
        return (tokens);
    }


    /**
     * Retuns the description.
     *
     * @return description.
     */
    public String getDescription() {
        // SBgen: Get variable
        return (description.get());
    }

    /**
     * Returns the enabled state.
     *
     * @return enabled state.
     */
    public boolean isEnabled() {
        // SBgen: Get variable
        return (enabled.get());
    }

    /**
     * Enables/disables the command.
     *
     * @param enabled true/false.
     */
    public void setEnabled(boolean enabled) {
        // SBgen: Assign variable
        this.enabled.set(enabled);
    }

    /**
     * Returns the parent entity.
     *
     * @return parent entity.
     */
    public Entity getParent() {
        // SBgen: Get variable
        return (parent);
    }

    /**
     * Internal use.
     *
     * @param parent parent.
     */
    protected void setParent(Entity parent) {
        // SBgen: Assign variable
        this.parent = parent;
    }

    /**
     * Returns the command executor.
     *
     * @return command executor.
     */
    public CommandExecutor getCommandExecutor() {
        // SBgen: Get variable
        return (commandExecutor);
    }

    /**
     * Returns whether the command is enabled for the SwiftMQ Explorer.
     *
     * @return true/false.
     */
    public boolean isGuiEnabled() {
        return (guiEnabled.get());
    }

    /**
     * Enables/disables this command for the SwiftMQ Explorer.
     *
     * @param guiEnabled true/false.
     */
    public void setGuiEnabled(boolean guiEnabled) {
        this.guiEnabled.set(guiEnabled);
    }

    /**
     * Returns whether this command is for childs of an EntityList.
     *
     * @return true/false.
     */
    public boolean isGuiForChild() {
        return (guiForChild.get());
    }

    /**
     * Enables/disables this command for the childs of an EntityList (SwiftMQ Explorer).
     * Enabled, the command is shown on every child instead of the EntityList itself.
     *
     * @param guiForChild description.
     */
    public void setGuiForChild(boolean guiForChild) {
        this.guiForChild.set(guiForChild);
    }

    /**
     * Compares this command's tokenized command with another one.
     *
     * @param cmd tokenized command.
     * @return true/false.
     */
    public boolean equals(String[] cmd) {
        if (tokens.length > cmd.length)
            return false;

        for (int i = 0; i < tokens.length; i++) {
            if (!tokens[i].equals(cmd[i]))
                return false;
        }
        return true;
    }

    private String quote(String s) {
        return "\"" + s + "\"";
    }

    public String toJson() {
        StringBuffer s = new StringBuffer();
        s.append("{");
        s.append(quote("name")).append(": ");
        s.append(quote(name.get())).append(", ");
        s.append(quote("description")).append(": ");
        s.append(quote(description.get())).append(", ");
        s.append(quote("guiForChild")).append(": ");
        s.append(guiForChild.get());
        s.append("}");
        return s.toString();
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("[Command, name=");
        s.append(name.get());
        s.append(", pattern=");
        s.append(pattern.get());
        s.append(", description=");
        s.append(description.get());
        s.append(", enabled=");
        s.append(enabled.get());
        s.append("]");
        return s.toString();
    }
}

