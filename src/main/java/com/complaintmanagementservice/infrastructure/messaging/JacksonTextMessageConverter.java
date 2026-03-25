package com.complaintmanagementservice.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.jspecify.annotations.NonNull;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

public class JacksonTextMessageConverter implements MessageConverter {

    private static final String TYPE_ID_PROPERTY = "_type";

    private final ObjectMapper objectMapper;

    public JacksonTextMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public @NonNull Message toMessage(@NonNull Object object, @NonNull Session session) throws JMSException {
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
    public @NonNull Object fromMessage(@NonNull Message message) throws JMSException {
        if (!(message instanceof TextMessage textMessage)) {
            throw new MessageConversionException("Expected a JMS TextMessage");
        }

        try {
            return objectMapper.readValue(textMessage.getText(), resolveTargetType(message));
        }
        catch (ClassNotFoundException | JsonProcessingException exception) {
            throw new MessageConversionException("Unable to deserialize JMS payload", exception);
        }
    }

    private @NonNull Class<?> resolveTargetType(@NonNull Message message) throws JMSException, ClassNotFoundException {
        String typeName = message.getStringProperty(TYPE_ID_PROPERTY);
        if (typeName == null || typeName.isBlank()) {
            throw new MessageConversionException("JMS payload type header is missing");
        }
        return Class.forName(typeName);
    }
}
