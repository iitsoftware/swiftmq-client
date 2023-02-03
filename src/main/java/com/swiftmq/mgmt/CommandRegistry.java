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
import com.swiftmq.tools.dump.DumpableFactory;
import com.swiftmq.tools.dump.Dumpalizer;
import com.swiftmq.util.SwiftUtilities;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A CommandRegistry object will be attached to Entities/EntityLists. It contains
 * all commands for that Entity.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2002, All Rights Reserved
 * @see Entity
 * @see EntityList
 */
public class CommandRegistry implements Dumpable {
    String contextName = null;
    Entity myEntity = null;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * @SBGen Collection of com.swiftmq.mgmt.Command
     */
    List commands = new ArrayList();

    transient CommandExecutor defaultCommand = null;

    /**
     * Creates a new CommandRegistry.
     *
     * @param name     the name of the context; will be used to display help content.
     * @param myEntity attached Entity.
     */
    public CommandRegistry(String name, Entity myEntity) {
        this.contextName = name;
        this.myEntity = myEntity;
        CommandExecutor helpExecutor = new CommandExecutor() {
            public String[] execute(String[] context, Entity entity, String[] command) {
                Authenticator authenticator = AuthenticatorHolder.threadLocal.get();
                ArrayList al = new ArrayList();
                al.add(TreeCommands.RESULT);
                al.add("Commands from " + contextName);
                al.add("");
                for (int i = 0; i < commands.size(); i++) {
                    Command c = (Command) commands.get(i);
                    if (c.isEnabled() && !c.getName().equals("help")) {
                        if (authenticator == null || authenticator.isCommandGranted(entity, c.getName()))
                            al.addAll(createHelpEntry(c.getPattern(), c.getDescription()));
                    }
                }
                if (defaultCommand != null) {
                    String[] r = defaultCommand.execute(context, entity, command);
                    if (r != null) {
                        al.add("");
                        for (int i = 0; i < r.length; i++)
                            al.add(r[i]);
                    }
                }
                String[] rArr = (String[]) al.toArray(new String[al.size()]);
                return rArr;
            }
        };
        Command cmd = new Command("help", "help", "List all available commands", true, helpExecutor);
        addCommand(cmd);
    }

    CommandRegistry() {
    }

    private List createHelpEntry(String pattern, String description) {
        List list = new ArrayList();
        String prefix = SwiftUtilities.fillToLength(" ", 33);
        if (pattern.length() > 33) {
            list.add(pattern);
        } else
            prefix = SwiftUtilities.fillToLength(pattern, 33);
        StringTokenizer t = new StringTokenizer(description, "\n");
        while (t.hasMoreTokens()) {
            list.add(prefix + t.nextToken());
            if (pattern.length() > 33)
                prefix = SwiftUtilities.fillToLength(" ", 33);
        }
        return list;
    }

    public int getDumpId() {
        return MgmtFactory.COMMANDREGISTRY;
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
        lock.readLock().lock();
        try {
            writeDump(out, contextName);
            if (myEntity != null) {
                out.writeByte(1);
                Dumpalizer.dump(out, myEntity);
            } else
                out.writeByte(0);
            out.writeInt(commands.size());
            for (int i = 0; i < commands.size(); i++) {
                Dumpalizer.dump(out, (Command) commands.get(i));
            }
        } finally {
            lock.readLock().unlock();
        }

    }

    public void readContent(DataInput in)
            throws IOException {
        lock.writeLock().lock();
        try {
            DumpableFactory factory = new MgmtFactory();
            contextName = readDump(in);
            if (in.readByte() == 1)
                myEntity = (Entity) Dumpalizer.construct(in, factory);
            commands = new ArrayList();
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                commands.add(Dumpalizer.construct(in, factory));
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Set the context name.
     *
     * @param name name.
     */
    public void setName(String name) {
        lock.writeLock().lock();
        try {
            this.contextName = name;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Set a default command executor.
     * It will executed if no other registered command matches in <code>executeCommand()</code>.
     *
     * @param defaultCommand default command executor.
     */
    public void setDefaultCommand(CommandExecutor defaultCommand) {
        lock.writeLock().lock();
        try {
            this.defaultCommand = defaultCommand;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns a list of all registered commands.
     *
     * @return command list.
     */
    public List getCommands() {
        lock.readLock().lock();
        try {
            return commands;
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Add a command.
     *
     * @param command command.
     */
    public void addCommand(Command command) {
        lock.writeLock().lock();
        try {
            if (!commands.contains(command))
                commands.add(command);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Remove a command.
     *
     * @param command command.
     */
    public void removeCommand(Command command) {
        lock.writeLock().lock();
        try {
            commands.remove(command);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Find a command.
     *
     * @param cmd tokenized command name.
     * @return command or null.
     */
    public Command findCommand(String[] cmd) {
        lock.readLock().lock();
        try {
            for (Object command : commands) {
                Command c = (Command) command;
                if (c.equals(cmd))
                    return c;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }

    }

    private boolean isCommonCommand(String cmd) {
        return cmd.equals("help") || cmd.equals(TreeCommands.DIR_CONTEXT) || cmd.equals(TreeCommands.CHANGE_CONTEXT) || cmd.equals(TreeCommands.AUTH);
    }

    /**
     * Execute a command.
     * If no command is found, the default command is executes. If no default command
     * has been registered, a error structure is returned ("Unknown command").
     *
     * @param context       the command context.
     * @param commandString the tokenized command string.
     * @return state structure.
     */
    public String[] executeCommand(String[] context, String[] commandString) {
        Authenticator authenticator = AuthenticatorHolder.threadLocal.get();
        if (authenticator != null && !isCommonCommand(commandString[0]) && !authenticator.isCommandGranted(myEntity, commandString[0]))
            return new String[]{TreeCommands.ERROR, "Command execution is not granted!"};
        Command c = findCommand(commandString);
        if (c != null && c.isEnabled())
            return c.getCommandExecutor().execute(context, myEntity, commandString);
        if (defaultCommand != null)
            return defaultCommand.execute(context, myEntity, commandString);
        return new String[]{TreeCommands.ERROR, "Unknown command"};
    }
}

