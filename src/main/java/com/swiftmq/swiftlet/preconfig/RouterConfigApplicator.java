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

import com.swiftmq.mgmt.XMLUtilities;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.Iterator;

/**
 * Applicator that applies preconfig operations to XML Documents (routerconfig.xml).
 * Used during startup before Swiftlets are initialized.
 */
public class RouterConfigApplicator extends AbstractApplicator {
    private Document routerConfig;
    private boolean changesMade = false;

    public RouterConfigApplicator(Document routerConfig) {
        this.routerConfig = routerConfig;
    }

    /**
     * Check if any changes were made to the router configuration.
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
     * Apply preconfig document to the router configuration.
     */
    public Document applyPreConfig(Document preconfig) throws Exception {
        Element root = preconfig.getRootElement();
        if (!root.getName().equals("router"))
            throw new Exception("Root element must be 'router'");

        processAttributes(routerConfig.getRootElement(), root);

        for (Iterator<Element> iter = root.elementIterator(); iter.hasNext(); ) {
            Element changeElement = iter.next();
            if (!(changeElement.getName().equals("swiftlet") || changeElement.getName().equals("ha-router")))
                throw new Exception("Next element after 'router' must be a 'ha-router' or a 'swiftlet' element!");

            if (changeElement.getName().equals("swiftlet")) {
                Attribute name = changeElement.attribute("name");
                if (name == null)
                    throw new Exception("Missing 'name' attribute in 'swiftlet' element!");

                Element configSwiftlet = getSwiftletElement(name.getValue());
                if (configSwiftlet == null) {
                    throw new Exception("Swiftlet with name '" + name.getValue() + "' not found!");
                }
                processElement("/" + name.getValue(), configSwiftlet, changeElement);
            } else {
                processElement("/ha-router", routerConfig.getRootElement().element("ha-router"), changeElement);
            }
        }

        return routerConfig;
    }

    private Element getSwiftletElement(String name) throws Exception {
        Element root = routerConfig.getRootElement();
        Element configSwiftlet = XMLUtilities.getSwiftletElement(name, root);
        if (configSwiftlet == null) {
            // Handle special swiftlets that might need to be created
            switch (name) {
                case "sys$filecache":
                    configSwiftlet = root.addElement("swiftlet");
                    configSwiftlet.addAttribute("name", name);
                    configSwiftlet.addElement("caches");
                    break;
                case "xt$amqpbridge":
                    configSwiftlet = root.addElement("swiftlet");
                    configSwiftlet.addAttribute("name", name);
                    configSwiftlet.addElement("bridges091");
                    configSwiftlet.addElement("bridges100");
                    break;
                case "xt$javamail":
                    configSwiftlet = root.addElement("swiftlet");
                    configSwiftlet.addAttribute("name", name);
                    configSwiftlet.addElement("inbound-bridges");
                    configSwiftlet.addElement("outbound-bridges");
                    break;
                case "xt$jmsbridge":
                    configSwiftlet = root.addElement("swiftlet");
                    configSwiftlet.addAttribute("name", name);
                    configSwiftlet.addElement("servers");
                    break;
                case "xt$replicator":
                    configSwiftlet = root.addElement("swiftlet");
                    configSwiftlet.addAttribute("name", name);
                    configSwiftlet.addElement("sinks");
                    configSwiftlet.addElement("sources");
                    break;
            }
        }
        return configSwiftlet;
    }

    private Element findElement(Element searchFor, Element searchIn) {
        Attribute nameAttr = searchFor.attribute("name");
        for (Iterator<Element> iter = searchIn.elementIterator(); iter.hasNext(); ) {
            Element child = iter.next();
            if (nameAttr != null) {
                if (hasName(nameAttr.getValue(), child))
                    return child;
            } else if (child.getName().equals(searchFor.getName()))
                return child;
        }
        return null;
    }

    private void clearElements(Element root) {
        for (Iterator<Element> iter = root.elementIterator(); iter.hasNext(); ) {
            Element child = iter.next();
            child.detach();
        }
    }

    @Override
    protected void handleAddOperation(String context, Object parent, Element changeElement) throws Exception {
        Element parentElement = (Element) parent;
        Element copy = changeElement.createCopy();
        copy.remove(copy.attribute(OP));

        // Add if not exists
        if (findElement(changeElement, parentElement) == null) {
            parentElement.add(copy);
            changesMade = true;
        }
    }

    @Override
    protected void handleRemoveOperation(String context, Object parent, Element changeElement) throws Exception {
        Element parentElement = (Element) parent;
        Element toRemove = findElement(changeElement, parentElement);
        if (toRemove != null) {
            toRemove.detach();
            changesMade = true;
        }
    }

    @Override
    protected void handleReplaceOperation(String context, Object parent, Element changeElement) throws Exception {
        Element parentElement = (Element) parent;
        Element existing = findElement(changeElement, parentElement);
        if (existing != null) {
            existing.detach();
        }
        Element copy = changeElement.createCopy();
        copy.remove(copy.attribute(OP));
        parentElement.add(copy);
        changesMade = true;
    }

    @Override
    protected void handleClearOperation(String context, Object target) throws Exception {
        Element element = (Element) target;
        int childCount = element.elements().size();
        if (childCount > 0) {
            clearElements(element);
            changesMade = true;
        }
    }

    @Override
    protected void applyAttributeChanges(String context, Object target, Element changeElement) throws Exception {
        Element configElement = (Element) target;
        processAttributes(configElement, changeElement);
    }

    private void processAttributes(Element configEle, Element changeEle) {
        for (Iterator<Attribute> iter = changeEle.attributeIterator(); iter.hasNext(); ) {
            Attribute attribute = iter.next();
            if (!attribute.getName().equals(OP)) {
                String newValue = attribute.getValue();
                Attribute configAttribute = configEle.attribute(attribute.getName());

                if (configAttribute == null) {
                    // New attribute
                    configEle.addAttribute(attribute.getName(), newValue);
                    changesMade = true;
                } else {
                    // Existing attribute - only update if value changed
                    String currentValue = configAttribute.getValue();
                    if ((currentValue == null && newValue != null) ||
                        (currentValue != null && !currentValue.equals(newValue))) {
                        configAttribute.setValue(newValue);
                        changesMade = true;
                    }
                }
            }
        }
    }

    @Override
    protected void processChildElements(String context, Object parent, Element changeElement) throws Exception {
        Element parentElement = (Element) parent;
        for (Iterator<Element> iter = changeElement.elementIterator(); iter.hasNext(); ) {
            Element changeChild = iter.next();
            String op = getOp(changeChild);

            if (op != null) {
                // Operations (add/remove/replace/clear) always work at parent level
                processElement(context + "/" + getEntityName(changeChild), parentElement, changeChild);
            } else {
                // No operation - recurse into existing element or parent for attribute changes
                Element configChild = findElement(changeChild, parentElement);
                if (configChild != null)
                    processElement(context + "/" + getEntityName(changeChild), configChild, changeChild);
                else
                    processElement(context + "/" + getEntityName(changeChild), parentElement, changeChild);
            }
        }
    }

    @Override
    protected void trace(String context, String message) {
        // No tracing for XML operations (startup phase)
    }

    @Override
    protected void logInfo(String context, String message) {
        // Log to stdout for startup processing
        System.out.println(message);
    }

    @Override
    protected void logWarning(String context, String message) {
        System.err.println("WARNING [" + context + "]: " + message);
    }

    @Override
    protected void logError(String context, String message) {
        System.err.println("ERROR [" + context + "]: " + message);
    }
}
