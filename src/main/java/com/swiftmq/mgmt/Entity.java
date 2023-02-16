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

import javax.swing.*;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

/**
 * A Entity represents a node within the management tree. It may contain Property objects,
 * as well as sub-entities. Each Entity must have a CommandRegistry where commands are
 * registered to be performed on that Entity or their childs.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2002, All Rights Reserved
 */
public class Entity implements Dumpable {
    public static final String SET_COMMAND = "set";
    protected DumpableFactory factory = new MgmtFactory();
    String name = null;
    String displayName = null;
    String description = null;
    boolean dynamic = false;
    transient Object userObject = null;
    transient Object dynamicObject = null;
    String[] dynamicPropNames = null;
    CommandRegistry commandRegistry = null;
    Entity parent = null;
    Map properties = null;
    String state = null;
    byte[] imageArray = null;
    transient ImageIcon imageIcon = null;
    transient String iconFilename = null;
    Map entities = null;
    transient EntityAddListener entityAddListener;
    transient EntityRemoveListener entityRemoveListener;
    transient List watchListeners = null;
    transient boolean upgrade = false;
    volatile String[] _ctx = null;
    volatile String[] _dctx = null;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates a new Entity.
     *
     * @param name        the name of the entity.
     * @param displayName the display name.
     * @param description a description.
     * @param state       the state (not used at the moment).
     */
    public Entity(String name, String displayName, String description, String state) {
        // SBgen: Assign variables
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.state = state;
        // SBgen: End assign
        entities = new CloneableMap();
        properties = new CloneableMap();
    }

    protected Entity() {
        this(null, null, null, null);
    }

    public int getDumpId() {
        return MgmtFactory.ENTITY;
    }

    protected boolean isSetParent() {
        return true;
    }

    protected void writeDump(DataOutput out, String s) throws IOException {
        if (s == null)
            out.writeByte(0);
        else {
            out.writeByte(1);
            out.writeUTF(s);
        }
    }

    protected String readDump(DataInput in) throws IOException {
        byte set = in.readByte();
        if (set == 1)
            return in.readUTF();
        return null;
    }

    protected void writeDump(DataOutput out, String[] s) throws IOException {
        if (s == null)
            out.writeByte(0);
        else {
            out.writeByte(1);
            out.writeInt(s.length);
            for (int i = 0; i < s.length; i++)
                out.writeUTF(s[i]);
        }
    }

    protected String[] readDumpStringArray(DataInput in) throws IOException {
        byte set = in.readByte();
        if (set == 1) {
            String[] s = new String[in.readInt()];
            for (int i = 0; i < s.length; i++)
                s[i] = in.readUTF();
            return s;
        }
        return null;
    }

    protected void writeDump(DataOutput out, byte[] s) throws IOException {
        if (s == null)
            out.writeByte(0);
        else {
            out.writeByte(1);
            out.writeInt(s.length);
            out.write(s);
        }
    }

    protected byte[] readDumpByteArray(DataInput in) throws IOException {
        byte set = in.readByte();
        if (set == 1) {
            byte[] s = new byte[in.readInt()];
            in.readFully(s);
            return s;
        }
        return null;
    }

    protected void writeDump(DataOutput out, Dumpable d) throws IOException {
        if (d == null)
            out.writeByte(0);
        else {
            out.writeByte(1);
            Dumpalizer.dump(out, d);
        }
    }

    protected Dumpable readDumpDumpable(DataInput in, DumpableFactory factory) throws IOException {
        byte set = in.readByte();
        if (set == 1) {
            return Dumpalizer.construct(in, factory);
        }
        return null;
    }

    protected void writeDump(DataOutput out, Map map) throws IOException {
        if (map == null)
            out.writeByte(0);
        else {
            out.writeByte(1);
            out.writeInt(map.size());
            for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); ) {
                Dumpalizer.dump(out, (Dumpable) ((Map.Entry) iter.next()).getValue());
            }
        }
    }

    protected CloneableMap readDumpDumpablePropMap(DataInput in, DumpableFactory factory) throws IOException {
        byte set = in.readByte();
        if (set == 1) {
            CloneableMap map = new CloneableMap();
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                Property prop = (Property) Dumpalizer.construct(in, factory);
                prop.setParent(this);
                map.put(prop.getName(), prop);
            }
            return map;
        }
        return null;
    }

    protected CloneableMap readDumpDumpableEntityMap(DataInput in, DumpableFactory factory) throws IOException {
        byte set = in.readByte();
        if (set == 1) {
            CloneableMap map = new CloneableMap();
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                Entity entity = (Entity) Dumpalizer.construct(in, factory);
                if (isSetParent())
                    entity.setParent(this);
                map.put(entity.getName(), entity);
            }
            return map;
        }
        return null;
    }

    public void writeContent(DataOutput out)
            throws IOException {
        lock.readLock().lock();
        try {
            writeDump(out, name);
            writeDump(out, displayName);
            writeDump(out, description);
            writeDump(out, state);
            out.writeBoolean(dynamic);
            writeDump(out, dynamicPropNames);
            writeDump(out, commandRegistry);
            writeDump(out, imageArray);
            writeDump(out, properties);
            writeDump(out, entities);
        } finally {
            lock.readLock().unlock();
        }

    }

    public void readContent(DataInput in)
            throws IOException {
        lock.writeLock().lock();
        try {
            name = readDump(in);
            displayName = readDump(in);
            description = readDump(in);
            state = readDump(in);
            dynamic = in.readBoolean();
            dynamicPropNames = readDumpStringArray(in);
            commandRegistry = (CommandRegistry) readDumpDumpable(in, factory);
            imageArray = readDumpByteArray(in);
            properties = readDumpDumpablePropMap(in, factory);
            entities = readDumpDumpableEntityMap(in, factory);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Internal use only.
     */
    public void setImageArray(byte[] array) {
        lock.writeLock().lock();
        try {
            this.imageArray = array;
        } finally {
            lock.writeLock().unlock();
        }

    }

    String getIconFilename() {
        lock.readLock().lock();
        try {
            return iconFilename;
        } finally {
            lock.readLock().unlock();
        }

    }

    void setIconFilename(String iconFilename) {
        lock.writeLock().lock();
        try {
            this.iconFilename = iconFilename;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Internal use only.
     */
    public ImageIcon getIcon() {
        lock.readLock().lock();
        try {
            if (imageArray == null)
                return null;
            if (imageIcon == null)
                imageIcon = new ImageIcon(imageArray);
            return imageIcon;
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Internal use only.
     */
    public boolean isDynamic() {
        lock.readLock().lock();
        try {
            return dynamic;
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Internal use only.
     */
    public void setDynamic(boolean b) {
        lock.writeLock().lock();
        try {
            this.dynamic = b;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns the user object.
     *
     * @return user object.
     */
    public Object getUserObject() {
        lock.readLock().lock();
        try {
            return userObject;
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Attach a user object to this entity.
     *
     * @param userObject user object.
     */
    public void setUserObject(Object userObject) {
        lock.writeLock().lock();
        try {
            this.userObject = userObject;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns the dynamic object.
     *
     * @return dynamic object.
     */
    public Object getDynamicObject() {
        lock.readLock().lock();
        try {
            return dynamicObject;
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Attach a dynamic object to this entity.
     * In case this entity is dynamic (part of the usage list), and
     * there is a dynamic object which corresponds to this entity,
     * e.g. a connection object, this should be attached with this method.
     *
     * @param dynamicObject dynamic object.
     */
    public void setDynamicObject(Object dynamicObject) {
        lock.writeLock().lock();
        try {
            this.dynamicObject = dynamicObject;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns the dynamic property names.
     *
     * @return array of property names.
     */
    public String[] getDynamicPropNames() {
        lock.readLock().lock();
        try {
            return dynamicPropNames;
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Set an array of dynamic property names.
     * These are displayed in the dynamic chart of a dynamic entity,
     * each with a separate colored line. The type of these dynamic
     * properties must be of Integer, and, of course, the properties
     * must be added to this entity.
     *
     * @param dynamicPropNames array of property names.
     */
    public void setDynamicPropNames(String[] dynamicPropNames) {
        lock.writeLock().lock();
        try {
            this.dynamicPropNames = dynamicPropNames;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Internal use only.
     */
    public String[] getContext() {
        lock.readLock().lock();
        try {
            if (_ctx != null)
                return _ctx;
            List al = new ArrayList();
            Entity actEntity = this;
            while (actEntity != null) {
                al.add(actEntity.getName());
                actEntity = actEntity.getParent();
            }
            String[] ctx = new String[al.size()];
            int j = 0;
            for (int i = al.size() - 1; i >= 0; i--)
                ctx[j++] = (String) al.get(i);
            _ctx = ctx;
            return _ctx;
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Internal use only.
     */
    public String[] getDisplayContext() {
        lock.readLock().lock();
        try {
            if (_dctx != null)
                return _dctx;
            ArrayList al = new ArrayList();
            Entity actEntity = this;
            while (actEntity != null) {
                al.add(actEntity.getDisplayName());
                actEntity = actEntity.getParent();
            }
            String[] ctx = new String[al.size()];
            int j = 0;
            for (int i = al.size() - 1; i >= 0; i--)
                ctx[j++] = (String) al.get(i);
            _dctx = ctx;
            return _dctx;
        } finally {
            lock.readLock().unlock();
        }

    }


    /**
     * Creates the commands out of the command registry. Normally,
     * this is performed automatically, except for dynamic entities.
     *
     * @see EntityList
     */
    public void createCommands() {
        Map cloned = null;
        lock.readLock().lock();
        try {
            commandRegistry = new CommandRegistry("Context '" + name + "'", null);
            CommandExecutor setExecutor = new CommandExecutor() {
                public String[] execute(String[] context, Entity entity, String[] cmd) {
                    if (cmd.length < 2 || cmd.length > 3)
                        return new String[]{TreeCommands.ERROR, "Invalid command, please try '" + SET_COMMAND + " <prop> [<value>]'"};
                    String[] result = null;
                    Property p = getProperty(cmd[1]);
                    if (p == null)
                        result = new String[]{TreeCommands.ERROR, "Unknown Property: " + cmd[1]};
                    else if (p.isReadOnly())
                        result = new String[]{TreeCommands.ERROR, "Property is read-only."};
                    else {
                        try {
                            if (cmd.length == 2)
                                p.setValue(null);
                            else
                                p.setValue(Property.convertToType(p.getType(), cmd[2]));
                            if (p.isRebootRequired())
                                result = new String[]{TreeCommands.INFO, "To activate this Property Change, a Reboot of this Router is required."};
                        } catch (Exception e) {
                            result = new String[]{TreeCommands.ERROR, e.getMessage()};
                        }
                    }
                    return result;
                }
            };
            Command setCommand = new Command(SET_COMMAND, SET_COMMAND + " <prop> [<value>]", "Set Property <prop> to Value <value> or null", true, setExecutor);
            commandRegistry.addCommand(setCommand);
            CommandExecutor describeExecutor = new CommandExecutor() {
                private String check(Object o) {
                    return o == null ? "<not set>" : o.toString();
                }

                public String[] execute(String[] context, Entity entity, String[] cmd) {
                    if (cmd.length != 2)
                        return new String[]{TreeCommands.ERROR, "Invalid command, please try 'describe <prop>'"};
                    String[] result = null;
                    Property p = getProperty(cmd[1]);
                    if (p == null)
                        result = new String[]{TreeCommands.ERROR, "Unknown Property: " + cmd[1]};
                    else {
                        result = new String[13];
                        result[0] = TreeCommands.RESULT;
                        result[1] = "Property Name  : " + p.getName();
                        result[2] = "Display Name   : " + check(p.getDisplayName());
                        result[3] = "Description    : " + check(p.getDescription());
                        result[4] = "Type           : " + p.getType();
                        result[5] = "Min. Value     : " + check(p.getMinValue());
                        result[6] = "Max. Value     : " + check(p.getMaxValue());
                        result[7] = "Default Value  : " + check(p.getDefaultValue());
                        result[8] = "Poss. Values   : " + check(p.getPossibleValues());
                        result[9] = "Actual Value   : " + check(p.getValue());
                        result[10] = "Mandatory     : " + p.isMandatory();
                        result[11] = "Read Only      : " + p.isReadOnly();
                        result[12] = "Reboot Required: " + p.isRebootRequired();
                    }
                    return result;
                }
            };
            Command describeCommand = new Command("describe", "describe <prop>", "Show full Description of Property <prop>", true, describeExecutor);
            commandRegistry.addCommand(describeCommand);
            cloned = (Map) ((TreeMap) entities).clone();
        } finally {
            lock.readLock().unlock();
        }

        // Do it for all sub-entities
        for (Object o : cloned.entrySet()) {
            Entity entity = (Entity) ((Map.Entry) o).getValue();
            entity.createCommands();
        }
    }


    /**
     * Returns the command registry.
     *
     * @return command registry.
     */
    public CommandRegistry getCommandRegistry() {
        lock.readLock().lock();
        try {
            return commandRegistry;
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Returns the entity name.
     *
     * @return entity name.
     */
    public String getName() {
        lock.readLock().lock();
        try {
            return (name);
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Set the entity name.
     *
     * @param name name.
     */
    public void setName(String name) {
        lock.writeLock().lock();
        try {
            this.name = name;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns the display name.
     *
     * @return display name.
     */
    public String getDisplayName() {
        lock.readLock().lock();
        try {
            return (displayName);
        } finally {
            lock.readLock().unlock();
        }

    }


    /**
     * Returns the description.
     *
     * @return description.
     */
    public String getDescription() {
        lock.readLock().lock();
        try {
            return (description);
        } finally {
            lock.readLock().unlock();
        }

    }


    /**
     * Add a command to the command registry.
     *
     * @param name    command name.
     * @param command command.
     */
    public void addCommand(String name, Command command) {
        lock.writeLock().lock();
        try {
            command.setParent(this);
            commandRegistry.addCommand(command);
        } finally {
            lock.writeLock().unlock();
        }

    }


    /**
     * Remove a command from the command registry.
     *
     * @param name command name.
     */
    public void removeCommand(String name) {
        lock.writeLock().lock();
        try {
            Command cmd = commandRegistry.findCommand(new String[]{name});
            if (cmd != null)
                commandRegistry.removeCommand(cmd);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Internal use only.
     */
    public String getState() {
        lock.readLock().lock();
        try {
            return (state);
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Internal use only.
     */
    public void setState(String state) {
        lock.writeLock().lock();
        try {
            this.state = state;
        } finally {
            lock.writeLock().unlock();
        }

    }


    /**
     * Add a property.
     *
     * @param name     property name.
     * @param property property.
     */
    public void addProperty(String name, Property property) {
        lock.writeLock().lock();
        try {
            property.setParent(this);
            properties.put(name, property);
        } finally {
            lock.writeLock().unlock();
        }

    }


    /**
     * Remove a property.
     *
     * @param name property name.
     */
    public void removeProperty(String name) {
        lock.writeLock().lock();
        try {
            properties.remove(name);
        } finally {
            lock.writeLock().unlock();
        }

    }


    /**
     * Returns a property.
     *
     * @param name property name.
     * @return property.
     */
    public Property getProperty(String name) {
        lock.readLock().lock();
        try {
            return (Property) properties.get(name);
        } finally {
            lock.readLock().unlock();
        }

    }


    /**
     * Returns a Map of all properties.
     *
     * @return map of properties.
     */
    public Map getProperties() {
        lock.readLock().lock();
        try {
            return ((CloneableMap) properties).createCopy();
        } finally {
            lock.readLock().unlock();
        }

    }


    /**
     * Add an Entity.
     *
     * @param entity entity.
     * @throws EntityAddException thrown by an EntityAddListener.
     */
    public void addEntity(Entity entity)
            throws EntityAddException {
        EntityAddListener listener = null;
        lock.readLock().lock();
        try {
            listener = entityAddListener;
        } finally {
            lock.readLock().unlock();
        }
        if (listener != null)
            listener.onEntityAdd(this, entity);
        entity.setParent(this);

        lock.writeLock().lock();
        try {
            entities.put(entity.getName(), entity);
        } finally {
            lock.writeLock().unlock();
        }

        notifyEntityWatchListeners(true, entity);
    }


    /**
     * Removes an Entity.
     *
     * @param entity entity.
     * @throws EntityRemoveException thrown by an EntityRemoveListener.
     */
    public void removeEntity(Entity entity)
            throws EntityRemoveException {
        if (entity == null)
            return;
        EntityRemoveListener listener;
        lock.readLock().lock();
        try {
            listener = entityRemoveListener;
        } finally {
            lock.readLock().unlock();
        }
        if (listener != null)
            listener.onEntityRemove(this, entity);

        lock.writeLock().lock();
        try {
            entities.remove(entity.getName());
        } finally {
            lock.writeLock().unlock();
        }

        entity.setParent(null);
        notifyEntityWatchListeners(false, entity);
    }


    /**
     * Removes all Entities.
     */
    public void removeEntities() {
        lock.writeLock().lock();
        try {
            entities.clear();
        } finally {
            lock.writeLock().unlock();
        }

    }


    /**
     * Removes an Entity with that dynamic object set.
     *
     * @param dynamicObject dynamic object.
     */
    public void removeDynamicEntity(Object dynamicObject) {
        Entity entity = null;
        lock.readLock().lock();
        try {
            for (Iterator iter = entities.entrySet().iterator(); iter.hasNext(); ) {
                entity = (Entity) ((Map.Entry) iter.next()).getValue();
                if (entity.getDynamicObject() == dynamicObject) {
                    entity.setDynamicObject(null);
                    entity.setParent(null);
                    iter.remove();
                    break;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        if (entity != null)
            notifyEntityWatchListeners(false, entity);
    }


    /**
     * Returns a Sub-Entity.
     *
     * @param name name.
     * @return Entity.
     */
    public Entity getEntity(String name) {
        lock.readLock().lock();
        try {
            return (Entity) entities.get(name);
        } finally {
            lock.readLock().unlock();
        }

    }


    /**
     * Returns an array with all sub-entity names
     *
     * @return array with all sub-entity names.
     */
    public String[] getEntityNames() {
        lock.readLock().lock();
        try {
            if (entities.size() == 0)
                return null;
            String[] rArr = new String[entities.size()];
            int i = 0;
            for (Iterator iter = entities.keySet().iterator(); iter.hasNext(); )
                rArr[i++] = (String) iter.next();
            return rArr;
        } finally {
            lock.readLock().unlock();
        }

    }


    /**
     * Returns a Map with all Entities.
     *
     * @return entity map.
     */
    public Map getEntities() {
        lock.readLock().lock();
        try {
            return ((CloneableMap) entities).createCopy();
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Returns the parent Entity.
     *
     * @return parent Entity.
     */
    public Entity getParent() {
        lock.readLock().lock();
        try {
            return (parent);
        } finally {
            lock.readLock().unlock();
        }

    }

    protected void setParent(Entity parent) {
        lock.writeLock().lock();
        try {
            this.parent = parent;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns the EntityAddListener
     *
     * @return listener.
     */
    public EntityAddListener getEntityAddListener() {
        lock.readLock().lock();
        try {
            return (entityAddListener);
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Set the EntityAddListener.
     * There can only be 1 EntityAddListener which is responsible to verify the addition
     * and may be throw an EntityAddException.
     *
     * @param entityAddListener listener.
     */
    public void setEntityAddListener(EntityAddListener entityAddListener) {
        lock.writeLock().lock();
        try {
            this.entityAddListener = entityAddListener;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns the EntityRemoveListener
     *
     * @return listener.
     */
    public EntityRemoveListener getEntityRemoveListener() {
        lock.readLock().lock();
        try {
            return (entityRemoveListener);
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Set the EntityRemoveListener.
     * There can only be 1 EntityRemoveListener which is responsible to verify the removal
     * and may be throw an EntityRemoveException.
     *
     * @param entityRemoveListener listener.
     */
    public void setEntityRemoveListener(EntityRemoveListener entityRemoveListener) {
        lock.writeLock().lock();
        try {
            this.entityRemoveListener = entityRemoveListener;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Adds an EntityWatchListener.
     * There can be several of thos listeners registered at an Entity. They all are
     * informed on addition/removal of sub-entities after the action has been performed
     * (Entity added/removed).
     *
     * @param l listener.
     */
    public void addEntityWatchListener(EntityWatchListener l) {
        lock.writeLock().lock();
        try {
            if (watchListeners == null)
                watchListeners = new ArrayList();
            watchListeners.add(l);
        } finally {
            lock.writeLock().unlock();
        }

    }


    /**
     * Removes an EntityWatchListener.
     *
     * @param l listener.
     */
    public void removeEntityWatchListener(EntityWatchListener l) {
        lock.writeLock().lock();
        try {
            if (watchListeners != null)
                watchListeners.remove(l);
        } finally {
            lock.writeLock().unlock();
        }

    }

    private List copyOf(List in) {
        lock.readLock().lock();
        try {
            List out = new ArrayList();
            if (in != null)
                out.addAll(in);
            return out;
        } finally {
            lock.readLock().unlock();
        }
    }

    protected void notifyEntityWatchListeners(boolean entityAdded, Entity entity) {
        List copy = copyOf(watchListeners);
        IntStream.range(0, copy.size()).mapToObj(i -> (EntityWatchListener) watchListeners.get(i)).forEach(l -> {
            if (entityAdded)
                l.entityAdded(this, entity);
            else
                l.entityRemoved(this, entity);
        });
    }

    /**
     * Internal use only.
     */
    public Entity createCopy() {
        lock.readLock().lock();
        try {
            Entity entity = new Entity(name, displayName, description, state);
            entity.dynamic = dynamic;
            entity.dynamicPropNames = dynamicPropNames;
            entity.commandRegistry = commandRegistry;
            entity.properties = new CloneableMap();
            for (Object o : properties.entrySet()) {
                Property p = (Property) ((Map.Entry) o).getValue();
                Property copy = p.createCopy();
                copy.setParent(entity);
                entity.properties.put(copy.getName(), copy);
            }
            return entity;
        } finally {
            lock.readLock().unlock();
        }

    }

    public boolean isUpgrade() {
        lock.readLock().lock();
        try {
            return upgrade;
        } finally {
            lock.readLock().unlock();
        }

    }

    public void setUpgrade(boolean upgrade) {
        lock.writeLock().lock();
        try {
            this.upgrade = upgrade;
        } finally {
            lock.writeLock().unlock();
        }

    }

    protected String quote(String s) {
        return "\"" + s + "\"";
    }

    protected boolean commandIncluded(Command command, String[] exclude) {
        return IntStream.range(0, exclude.length).noneMatch(i -> exclude[i].equals(command.getName()));
    }

    public String toJson() {
        lock.readLock().lock();
        try {
            StringBuffer s = new StringBuffer();
            s.append("{");
            s.append(quote("nodetype")).append(": ");
            s.append(quote("entity")).append(", ");
            s.append(quote("name")).append(": ");
            s.append(quote(name)).append(", ");
            s.append(quote("displayName")).append(": ");
            s.append(quote(displayName)).append(", ");
            s.append(quote("description")).append(": ");
            s.append(quote(description)).append(", ");
            s.append(quote("hasChilds")).append(": ");
            s.append(entities != null && entities.size() > 0);
            if (properties != null) {
                s.append(", ");
                s.append(quote("properties")).append(": ");
                s.append("[");
                boolean first = true;
                for (Object o : properties.entrySet()) {
                    if (!first)
                        s.append(", ");
                    first = false;
                    Property p = (Property) ((Map.Entry) o).getValue();
                    s.append(p.toJson());
                }
                s.append("]");
            }
            if (entities != null) {
                s.append(", ");
                s.append(quote("entities")).append(": ");
                s.append("[");
                boolean first = true;
                for (Object o : entities.entrySet()) {
                    if (!first)
                        s.append(", ");
                    first = false;
                    Entity e = (Entity) ((Map.Entry) o).getValue();
                    s.append("{");
                    s.append(quote("nodetype")).append(": ");
                    if (e instanceof EntityList)
                        s.append(quote("entitylist")).append(", ");
                    else
                        s.append(quote("entity")).append(", ");
                    s.append(quote("name")).append(": ");
                    s.append(quote(e.getName())).append(", ");
                    s.append(quote("displayName")).append(": ");
                    s.append(quote(e.getDisplayName())).append(", ");
                    s.append(quote("description")).append(": ");
                    s.append(quote(e.getDescription())).append(", ");
                    s.append(quote("hasChilds")).append(": ");
                    if (e instanceof EntityList)
                        s.append(true);
                    else
                        s.append(e.getEntities() != null && e.getEntities().size() > 0);
                    s.append("}");

                }
                s.append("]");
            }
            if (commandRegistry != null && commandRegistry.getCommands() != null) {
                s.append(", ");
                s.append(quote("commands")).append(": ");
                s.append("[");
                List cmds = commandRegistry.getCommands();
                boolean first = true;
                for (int i = 0; i < cmds.size(); i++) {
                    Command command = (Command) cmds.get(i);
                    if (commandIncluded(command, new String[]{"help", "set", "describe"})) {
                        if (!first) {
                            s.append(", ");
                        }
                        first = false;
                        s.append(((Command) cmds.get(i)).toJson());
                    }
                }
                s.append("]");
            }
            s.append("}");
            return s.toString();
        } finally {
            lock.readLock().unlock();
        }

    }

    public String toString() {
        lock.readLock().lock();
        try {
            StringBuffer s = new StringBuffer();
            s.append("\n[Entity, name=");
            s.append(name);
            s.append(", displayName=");
            s.append(displayName);
            s.append(", description=");
            s.append(description);
            s.append(", state=");
            s.append(state);
            s.append(", properties=");
            s.append(properties);
            s.append(", entities=");
            s.append(entities);
            s.append("]");
            return s.toString();
        } finally {
            lock.readLock().unlock();
        }

    }

    protected class CloneableMap extends TreeMap {
        public CloneableMap createCopy() {
            return (CloneableMap) clone();
        }
    }
}

