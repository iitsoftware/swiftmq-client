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

import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.Iterator;

/**
 * Abstract base class for applying preconfig XML operations.
 * Subclasses implement specific destinations (XML Document or live management tree).
 */
public abstract class AbstractApplicator {
    protected static final String OP = "_op";

    /**
     * Get the operation attribute value from an element.
     */
    protected String getOp(Element element) {
        if (element.attribute(OP) == null)
            return null;
        return element.attribute(OP).getValue();
    }

    /**
     * Check if an element has a specific name attribute value.
     */
    protected boolean hasName(String name, Element element) {
        for (Iterator<Attribute> iter = element.attributeIterator(); iter.hasNext(); ) {
            Attribute attribute = iter.next();
            if (attribute.getName().equals("name") && attribute.getValue().equals(name))
                return true;
        }
        return false;
    }

    /**
     * Get the entity name from an element (from 'name' attribute or element name itself).
     */
    protected String getEntityName(Element element) {
        Attribute nameAttr = element.attribute("name");
        if (nameAttr != null)
            return nameAttr.getValue();
        return element.getName();
    }

    // Abstract methods to be implemented by subclasses for specific destinations

    /**
     * Handle _op="add" operation.
     */
    protected abstract void handleAddOperation(String context, Object parent, Element changeElement) throws Exception;

    /**
     * Handle _op="remove" operation.
     */
    protected abstract void handleRemoveOperation(String context, Object parent, Element changeElement) throws Exception;

    /**
     * Handle _op="replace" operation.
     */
    protected abstract void handleReplaceOperation(String context, Object parent, Element changeElement) throws Exception;

    /**
     * Handle _op="clear" operation.
     */
    protected abstract void handleClearOperation(String context, Object target) throws Exception;

    /**
     * Apply attribute changes (properties).
     */
    protected abstract void applyAttributeChanges(String context, Object target, Element changeElement) throws Exception;

    /**
     * Process child elements recursively.
     */
    protected abstract void processChildElements(String context, Object parent, Element changeElement) throws Exception;

    /**
     * Log trace message if tracing is enabled.
     */
    protected abstract void trace(String context, String message);

    /**
     * Log information message.
     */
    protected abstract void logInfo(String context, String message);

    /**
     * Log warning message.
     */
    protected abstract void logWarning(String context, String message);

    /**
     * Log error message.
     */
    protected abstract void logError(String context, String message);

    /**
     * Process a single XML element and apply to the target.
     */
    protected void processElement(String context, Object target, Element changeElement) throws Exception {
        if (target == null)
            return;

        String op = getOp(changeElement);

        // First apply attribute changes (unless it's add/remove which handle their own attributes)
        if (op == null || (!op.equals("add") && !op.equals("remove"))) {
            applyAttributeChanges(context, target, changeElement);
        }

        // Handle operations
        if (op != null) {
            switch (op) {
                case "add":
                    handleAddOperation(context, target, changeElement);
                    break;
                case "remove":
                    handleRemoveOperation(context, target, changeElement);
                    break;
                case "replace":
                    handleReplaceOperation(context, target, changeElement);
                    break;
                case "clear":
                    handleClearOperation(context, target);
                    break;
                default:
                    logWarning(context, "Unknown operation: " + op);
            }
        } else {
            // No operation, process child elements recursively
            processChildElements(context, target, changeElement);
        }
    }
}
