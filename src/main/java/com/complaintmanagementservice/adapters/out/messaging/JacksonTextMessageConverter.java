package com.complaintmanagementservice.adapters.out.messaging;

import com.complaintmanagementservice.adapters.in.messaging.dto.CreateComplaintQueueMessage;
import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintCreatedQueueMessage;
import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintSlaWarningQueueMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.jspecify.annotations.NullMarked;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

import java.util.Map;

@NullMarked
public class JacksonTextMessageConverter implements MessageConverter {

    private static final String TYPE_ID_PROPERTY = "_type";
    private static final Map<String, Class<?>> PAYLOAD_TYPES = Map.of(
            CreateComplaintQueueMessage.class.getName(), CreateComplaintQueueMessage.class,
            ComplaintCreatedQueueMessage.class.getName(), ComplaintCreatedQueueMessage.class,
            ComplaintSlaWarningQueueMessage.class.getName(), ComplaintSlaWarningQueueMessage.class
    );

    private final ObjectMapper objectMapper;

    public JacksonTextMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public Message toMessage(Object object, Session session) throws JMSException {
        try {
            TextMessage message = session.createTextMessage(objectMapper.writeValueAsString(object));
            message.setStringProperty(TYPE_ID_PROPERTY, object.getClass().getName());
            return message;
        }
        catch (JsonProcessingException exception) {
            throw new MessageConversionException("Unable to serialize JMS payload", exception);
        }
    }

    @Override
    public Object fromMessage(Message message) throws JMSException {
        try {
            TextMessage textMessage = asTextMessage(message);
            return objectMapper.readValue(textMessage.getText(), resolveTargetType(textMessage));
        }
        catch (JsonProcessingException exception) {
            throw new MessageConversionException("Unable to deserialize JMS payload", exception);
        }
    }

    private TextMessage asTextMessage(Message message) {
        if (message instanceof TextMessage textMessage) {
            return textMessage;
        }
        throw new MessageConversionException("Expected a JMS TextMessage");
    }

    private Class<?> resolveTargetType(Message message) throws JMSException {
        String typeName = message.getStringProperty(TYPE_ID_PROPERTY);
        if (typeName == null || typeName.isBlank()) {
            throw new MessageConversionException("JMS payload type header is missing");
        }
        Class<?> targetType = PAYLOAD_TYPES.get(typeName);
        if (targetType == null) {
            throw new MessageConversionException("Unsupported JMS payload type");
        }
        return targetType;
    }
}
