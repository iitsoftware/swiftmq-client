package com.swiftmq.jms.v750;

import com.swiftmq.jms.*;

import javax.jms.*;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JMSProducerImpl implements JMSProducer {
    MessageProducerImpl producer;
    CompletionListener completionListener = null;
    MessageImpl properties = new MessageImpl();

    public JMSProducerImpl(MessageProducerImpl producer) {
        this.producer = producer;
    }

    @Override
    public JMSProducer setDisableMessageID(boolean b) {
        try {
            producer.setDisableMessageID(b);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public boolean getDisableMessageID() {
        try {
            return producer.getDisableMessageID();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSProducer setDisableMessageTimestamp(boolean b) {
        try {
            producer.setDisableMessageTimestamp(b);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public boolean getDisableMessageTimestamp() {
        try {
            return producer.getDisableMessageTimestamp();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSProducer setDeliveryMode(int i) {
        try {
            producer.setDeliveryMode(i);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public int getDeliveryMode() {
        try {
            return producer.getDeliveryMode();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSProducer setPriority(int i) {
        try {
            producer.setPriority(i);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public int getPriority() {
        try {
            return producer.getPriority();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSProducer setTimeToLive(long l) {
        try {
            producer.setTimeToLive(l);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public long getTimeToLive() {
        try {
            return producer.getTimeToLive();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSProducer setDeliveryDelay(long l) {
        try {
            producer.setDeliveryDelay(l);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public long getDeliveryDelay() {
        try {
            return producer.getDeliveryDelay();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSProducer setAsync(CompletionListener completionListener) {
        this.completionListener = completionListener;
        return this;
    }

    @Override
    public CompletionListener getAsync() {
        return completionListener;
    }

    @Override
    public JMSProducer setProperty(String s, boolean b) {
        try {
            properties.setBooleanProperty(s, b);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public JMSProducer setProperty(String s, byte b) {
        try {
            properties.setByteProperty(s, b);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public JMSProducer setProperty(String s, short i) {
        try {
            properties.setShortProperty(s, i);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public JMSProducer setProperty(String s, int i) {
        try {
            properties.setIntProperty(s, i);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public JMSProducer setProperty(String s, long l) {
        try {
            properties.setLongProperty(s, l);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public JMSProducer setProperty(String s, float v) {
        try {
            properties.setFloatProperty(s, v);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public JMSProducer setProperty(String s, double v) {
        try {
            properties.setDoubleProperty(s, v);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public JMSProducer setProperty(String s, String v) {
        try {
            properties.setStringProperty(s, v);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public JMSProducer setProperty(String s, Object o) {
        try {
            properties.setObjectProperty(s, o);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public JMSProducer clearProperties() {
        try {
            properties.clearProperties();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public boolean propertyExists(String s) {
        try {
            return properties.propertyExists(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean getBooleanProperty(String s) {
        try {
            return properties.getBooleanProperty(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public byte getByteProperty(String s) {
        try {
            return properties.getByteProperty(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public short getShortProperty(String s) {
        try {
            return properties.getShortProperty(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public int getIntProperty(String s) {
        try {
            return properties.getIntProperty(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public long getLongProperty(String s) {
        try {
            return properties.getLongProperty(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public float getFloatProperty(String s) {
        try {
            return properties.getFloatProperty(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public double getDoubleProperty(String s) {
        try {
            return properties.getDoubleProperty(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public String getStringProperty(String s) {
        try {
            return properties.getStringProperty(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public Object getObjectProperty(String s) {
        try {
            return properties.getObjectProperty(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public Set<String> getPropertyNames() {
        try {
            Set<String> result = new HashSet<>();
            for (Enumeration names = properties.getPropertyNames(); names.hasMoreElements(); )
                result.add((String) names.nextElement());
            return result;
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSProducer setJMSCorrelationIDAsBytes(byte[] bytes) {
        try {
            properties.setJMSCorrelationIDAsBytes(bytes);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public byte[] getJMSCorrelationIDAsBytes() {
        try {
            return properties.getJMSCorrelationIDAsBytes();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSProducer setJMSCorrelationID(String s) {
        try {
            properties.setJMSCorrelationID(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public String getJMSCorrelationID() {
        try {
            return properties.getJMSCorrelationID();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSProducer setJMSType(String s) {
        try {
            properties.setJMSType(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public String getJMSType() {
        try {
            return properties.getJMSType();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSProducer setJMSReplyTo(Destination destination) {
        try {
            properties.setJMSReplyTo(destination);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public Destination getJMSReplyTo() {
        try {
            return properties.getJMSReplyTo();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    private void setHeaderAndProps(Message message) {
        try {
            // Header
            message.setJMSCorrelationID(properties.getJMSCorrelationID());
            message.setJMSType(properties.getJMSType());
            message.setJMSReplyTo(properties.getJMSReplyTo());

            // Properties
            for (Enumeration names = properties.getPropertyNames(); names.hasMoreElements(); ) {
                String name = (String) names.nextElement();
                message.setObjectProperty(name, properties.getObjectProperty(name));
            }
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSProducer send(Destination destination, Message message) {
        setHeaderAndProps(message);
        try {
            producer.send(destination, message, completionListener);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        return this;
    }

    @Override
    public JMSProducer send(Destination destination, String s) {
        TextMessage textMessage = new TextMessageImpl();
        try {
            textMessage.setText(s);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        send(destination, textMessage);
        return this;
    }

    @Override
    public JMSProducer send(Destination destination, Map<String, Object> map) {
        MapMessage mapMessage = new MapMessageImpl();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            try {
                mapMessage.setObject(key, value);
            } catch (JMSException e) {
                throw new JMSRuntimeException(e.getMessage());
            }
        }
        send(destination, mapMessage);
        return this;
    }

    @Override
    public JMSProducer send(Destination destination, byte[] bytes) {
        BytesMessage bytesMessage = new BytesMessageImpl();
        try {
            bytesMessage.writeBytes(bytes);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        send(destination, bytesMessage);
        return this;
    }

    @Override
    public JMSProducer send(Destination destination, Serializable serializable) {
        ObjectMessage objectMessage = new ObjectMessageImpl();
        try {
            objectMessage.setObject(serializable);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
        send(destination, objectMessage);
        return this;
    }
}
