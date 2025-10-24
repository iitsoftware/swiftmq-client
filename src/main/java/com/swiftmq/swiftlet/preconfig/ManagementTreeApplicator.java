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

package com.swiftmq.swiftlet.preconfig;

import com.swiftmq.mgmt.*;
import com.swiftmq.swiftlet.log.LogSwiftlet;
import com.swiftmq.swiftlet.trace.TraceSpace;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.Iterator;
import java.util.Map;

/**
 * Applicator that applies preconfig operations to the live management tree (RouterConfigInstance).
 * Used during runtime after Swiftlets are initialized. Thread-safe through the management tree's own locking.
 */
public class ManagementTreeApplicator extends AbstractApplicator {
    TraceSpace traceSpace = null;
    LogSwiftlet logSwiftlet = null;
    private boolean changesMade = false;

    public ManagementTreeApplicator(TraceSpace traceSpace, LogSwiftlet logSwiftlet) {
        this.traceSpace = traceSpace;
        this.logSwiftlet = logSwiftlet;
    }

    /**
     * Check if any changes were made to the management tree.
     */
    public boolean hasChangesMade() {
        return changesMade;
    }

    /**
     * Reset the changes flag.
     */
    public void resetChangesFlag() {
        changesMade = false;
    }

    /**
     * Apply preconfig document to the management tree.
     */
    public void applyPreConfig(Document preconfig, String context) throws Exception {
        Element root = preconfig.getRootElement();
        if (!root.getName().equals("router"))
            throw new Exception("Root element must be 'router'");

        for (Iterator<Element> iter = root.elementIterator(); iter.hasNext(); ) {
            Element element = iter.next();
            if (element.getName().equals("swiftlet")) {
                Attribute nameAttr = element.attribute("name");
                if (nameAttr == null)
                    throw new Exception("Missing 'name' attribute in 'swiftlet' element!");
                String swiftletName = nameAttr.getValue();

                if (traceSpace != null && traceSpace.enabled)
                    traceSpace.trace(context, "Processing swiftlet: " + swiftletName);

                Configuration config = (Configuration) RouterConfiguration.Singleton().getConfigurations().get(swiftletName);
                if (config == null) {
                    if (logSwiftlet != null)
                        logSwiftlet.logWarning(context, "Swiftlet '" + swiftletName + "' not found in configuration");
                    continue;
                }

                processElement(context + "/" + swiftletName, config, element);
            } else if (element.getName().equals("ha-router")) {
                if (traceSpace != null && traceSpace.enabled)
                    traceSpace.trace(context, "Processing ha-router configuration");
                // HA router processing would go here
            }
        }
    }

    @Override
    protected void handleAddOperation(String context, Object parent, Element changeElement) throws Exception {
        Entity parentEntity = (Entity) parent;
        String entityName = getEntityName(changeElement);

        trace(context, "Adding entity: " + entityName);

        // Check if entity already exists
        Entity existingEntity = parentEntity.getEntity(entityName);
        if (existingEntity != null) {
            trace(context, "Entity already exists, skipping: " + entityName);
            return;
        }

        // Create new entity using the EntityList's template if parent is an EntityList
        Entity newEntity;
        if (parentEntity instanceof EntityList) {
            EntityList entityList = (EntityList) parentEntity;
            newEntity = entityList.createEntity();
            if (newEntity == null) {
                logError(context, "Failed to create entity from EntityList template");
                throw new Exception("Failed to create entity from template");
            }
            trace(context, "Created entity from EntityList template");
        } else {
            logError(context, "Parent is not an EntityList, cannot add entity: " + entityName);
            throw new Exception("Parent is not an EntityList");
        }

        // Set the name
        newEntity.setName(entityName);

        // Create commands for the entity
        newEntity.createCommands();
        trace(context, "Created commands for entity: " + entityName);

        // Update property values from XML attributes
        for (Iterator<Attribute> iter = changeElement.attributeIterator(); iter.hasNext(); ) {
            Attribute attr = iter.next();
            String attrName = attr.getName();
            if (!attrName.equals("name") && !attrName.equals(OP)) {
                Property prop = newEntity.getProperty(attrName);
                if (prop != null) {
                    String newValue = attr.getValue();
                    try {
                        Object typedValue = Property.convertToType(prop.getType(), newValue);
                        prop.setValue(typedValue);
                        trace(context, "Set property '" + attrName + "' to '" + newValue + "'");
                    } catch (Exception e) {
                        logError(context, "Failed to set property '" + attrName + "': " + e.getMessage());
                    }
                } else {
                    logWarning(context, "Property '" + attrName + "' not found in entity template");
                }
            }
        }

        try {
            parentEntity.addEntity(newEntity);
            changesMade = true;
            logInfo(context, "Added entity: " + entityName);
        } catch (EntityAddException e) {
            logError(context, "Failed to add entity '" + entityName + "': " + e.getMessage());
            throw e;
        }
    }

    @Override
    protected void handleRemoveOperation(String context, Object parent, Element changeElement) throws Exception {
        Entity parentEntity = (Entity) parent;
        String entityName = getEntityName(changeElement);

        trace(context, "Removing entity: " + entityName);

        Entity entityToRemove = parentEntity.getEntity(entityName);
        if (entityToRemove == null) {
            trace(context, "Entity not found, cannot remove: " + entityName);
            return;
        }

        try {
            parentEntity.removeEntity(entityToRemove);
            changesMade = true;
            logInfo(context, "Removed entity: " + entityName);
        } catch (EntityRemoveException e) {
            logError(context, "Failed to remove entity '" + entityName + "': " + e.getMessage());
            throw e;
        }
    }

    @Override
    protected void handleReplaceOperation(String context, Object parent, Element changeElement) throws Exception {
        Entity parentEntity = (Entity) parent;
        String entityName = getEntityName(changeElement);

        trace(context, "Replacing entity: " + entityName);

        // Find the existing entity
        Entity existingEntity = parentEntity.getEntity(entityName);

        // If the element has child elements and the target is an EntityList, clear and repopulate
        if (changeElement.elements().size() > 0 && existingEntity instanceof EntityList) {
            trace(context, "Replace operation on EntityList with children - clearing and repopulating");
            EntityList entityList = (EntityList) existingEntity;

            // Clear existing entities
            handleClearOperation(context, entityList);

            // Process child elements (add them)
            processChildElements(context, entityList, changeElement);

            changesMade = true;
            logInfo(context, "Replaced entities in EntityList: " + entityName);
            return;
        }

        // Otherwise, remove and recreate the entity
        if (existingEntity != null) {
            try {
                parentEntity.removeEntity(existingEntity);
            } catch (EntityRemoveException e) {
                logError(context, "Failed to remove entity '" + entityName + "' for replacement: " + e.getMessage());
                throw e;
            }
        }

        // Create new entity using the EntityList's template if parent is an EntityList
        Entity newEntity;
        if (parentEntity instanceof EntityList) {
            EntityList entityList = (EntityList) parentEntity;
            newEntity = entityList.createEntity();
            if (newEntity == null) {
                logError(context, "Failed to create entity from EntityList template");
                throw new Exception("Failed to create entity from template");
            }
            trace(context, "Created entity from EntityList template");
        } else {
            logError(context, "Parent is not an EntityList, cannot replace entity: " + entityName);
            throw new Exception("Parent is not an EntityList");
        }

        // Set the name
        newEntity.setName(entityName);

        // Create commands for the entity
        newEntity.createCommands();
        trace(context, "Created commands for entity: " + entityName);

        // Update property values from XML attributes
        for (Iterator<Attribute> iter = changeElement.attributeIterator(); iter.hasNext(); ) {
            Attribute attr = iter.next();
            String attrName = attr.getName();
            if (!attrName.equals("name") && !attrName.equals(OP)) {
                Property prop = newEntity.getProperty(attrName);
                if (prop != null) {
                    String newValue = attr.getValue();
                    try {
                        Object typedValue = Property.convertToType(prop.getType(), newValue);
                        prop.setValue(typedValue);
                        trace(context, "Set property '" + attrName + "' to '" + newValue + "'");
                    } catch (Exception e) {
                        logError(context, "Failed to set property '" + attrName + "': " + e.getMessage());
                    }
                } else {
                    logWarning(context, "Property '" + attrName + "' not found in entity template");
                }
            }
        }

        try {
            parentEntity.addEntity(newEntity);
            changesMade = true;
            logInfo(context, "Replaced entity: " + entityName);
        } catch (EntityAddException e) {
            logError(context, "Failed to add replacement entity '" + entityName + "': " + e.getMessage());
            throw e;
        }
    }

    @Override
    protected void handleClearOperation(String context, Object target) throws Exception {
        Entity entity = (Entity) target;

        trace(context, "Clearing all child entities");

        Map entities = entity.getEntities();
        if (entities != null && entities.size() > 0) {
            // Create a copy to avoid concurrent modification
            Entity[] entitiesToRemove = (Entity[]) entities.values().toArray(new Entity[0]);
            for (Entity child : entitiesToRemove) {
                try {
                    entity.removeEntity(child);
                    changesMade = true;
                } catch (EntityRemoveException e) {
                    logError(context, "Failed to remove entity '" + child.getName() + "' during clear: " + e.getMessage());
                }
            }
            if (changesMade) {
                logInfo(context, "Cleared all child entities");
            }
        }
    }

    @Override
    protected void applyAttributeChanges(String context, Object target, Element changeElement) throws Exception {
        Entity entity = (Entity) target;

        for (Iterator<Attribute> iter = changeElement.attributeIterator(); iter.hasNext(); ) {
            Attribute attribute = iter.next();
            String attrName = attribute.getName();

            // Skip special attributes
            if (attrName.equals(OP) || attrName.equals("name"))
                continue;

            Property property = entity.getProperty(attrName);
            if (property == null) {
                trace(context, "Property not found: " + attrName);
                continue;
            }

            String newValue = attribute.getValue();
            Object currentValue = property.getValue();
            String currentValueStr = currentValue != null ? currentValue.toString() : null;

            // Only update if value changed
            if ((currentValueStr == null && newValue != null) ||
                (currentValueStr != null && !currentValueStr.equals(newValue))) {

                trace(context, "Updating property '" + attrName + "' from '" + currentValueStr + "' to '" + newValue + "'");

                try {
                    Object typedValue = Property.convertToType(property.getType(), newValue);
                    property.setValue(typedValue);
                    changesMade = true;
                    logInfo(context, "Updated property '" + attrName + "' to '" + newValue + "'");

                    if (property.isRebootRequired()) {
                        logWarning(context, "Property '" + attrName + "' change requires a router reboot!");
                    }
                } catch (Exception e) {
                    logError(context, "Failed to set property '" + attrName + "': " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void processChildElements(String context, Object parent, Element changeElement) throws Exception {
        Entity parentEntity = (Entity) parent;

        for (Iterator<Element> iter = changeElement.elementIterator(); iter.hasNext(); ) {
            Element childElement = iter.next();
            String childName = getEntityName(childElement);
            String op = getOp(childElement);

            if (op != null) {
                // Operations (add/remove/replace/clear) always work at parent level
                processElement(context + "/" + childName, parentEntity, childElement);
            } else {
                // No operation - recurse into existing entity or warn if not found
                Entity childEntity = parentEntity.getEntity(childName);
                if (childEntity != null) {
                    processElement(context + "/" + childName, childEntity, childElement);
                } else {
                    logWarning(context, "Child entity '" + childName + "' not found");
                }
            }
        }
    }


    @Override
    protected void trace(String context, String message) {
        if (traceSpace != null && traceSpace.enabled)
            traceSpace.trace(context, message);
    }

    @Override
    protected void logInfo(String context, String message) {
        if (logSwiftlet != null)
            logSwiftlet.logInformation(context, message);
    }

    @Override
    protected void logWarning(String context, String message) {
        if (logSwiftlet != null)
            logSwiftlet.logWarning(context, message);
    }

    @Override
    protected void logError(String context, String message) {
        if (logSwiftlet != null)
            logSwiftlet.logError(context, message);
    }
}
