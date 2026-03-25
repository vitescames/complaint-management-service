package com.complaintmanagementservice.infrastructure.messaging;

import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintCreatedQueueMessage;
import org.junit.jupiter.api.Test;
import org.springframework.jms.support.converter.MessageConversionException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JacksonTextMessageConverterTest {

    @Test
    void shouldRejectUnsupportedSerializationPayload() throws Exception {
        JacksonTextMessageConverter converter = new JacksonTextMessageConverter(new com.fasterxml.jackson.databind.ObjectMapper());
        jakarta.jms.Session session = mock(jakarta.jms.Session.class);
        jakarta.jms.TextMessage textMessage = mock(jakarta.jms.TextMessage.class);
        when(session.createTextMessage(anyString())).thenReturn(textMessage);
        RecursivePayload payload = new RecursivePayload();

        assertThatThrownBy(() -> converter.toMessage(payload, session))
                .isInstanceOf(MessageConversionException.class)
                .hasMessage("Unable to serialize JMS payload");
    }

    @Test
    void shouldRejectNonTextMessagesAndMissingTypeHeader() throws Exception {
        JacksonTextMessageConverter converter = new JacksonTextMessageConverter(new com.fasterxml.jackson.databind.ObjectMapper());
        jakarta.jms.Message message = mock(jakarta.jms.Message.class);
        jakarta.jms.TextMessage textMessage = mock(jakarta.jms.TextMessage.class);
        when(textMessage.getStringProperty("_type")).thenReturn(null);
        when(textMessage.getText()).thenReturn("{}");

        assertThatThrownBy(() -> converter.fromMessage(message))
                .isInstanceOf(MessageConversionException.class)
                .hasMessage("Expected a JMS TextMessage");
        assertThatThrownBy(() -> converter.fromMessage(textMessage))
                .isInstanceOf(MessageConversionException.class)
                .hasMessage("JMS payload type header is missing");

        when(textMessage.getStringProperty("_type")).thenReturn(" ");
        assertThatThrownBy(() -> converter.fromMessage(textMessage))
                .isInstanceOf(MessageConversionException.class)
                .hasMessage("JMS payload type header is missing");
    }

    @Test
    void shouldRejectInvalidDeserializationPayload() throws Exception {
        JacksonTextMessageConverter converter = new JacksonTextMessageConverter(new com.fasterxml.jackson.databind.ObjectMapper());
        jakarta.jms.TextMessage textMessage = mock(jakarta.jms.TextMessage.class);
        when(textMessage.getStringProperty("_type")).thenReturn(ComplaintCreatedQueueMessage.class.getName());
        when(textMessage.getText()).thenReturn("not-json");

        assertThatThrownBy(() -> converter.fromMessage(textMessage))
                .isInstanceOf(MessageConversionException.class)
                .hasMessage("Unable to deserialize JMS payload");
    }

    private static final class RecursivePayload {
        private final UUID id = UUID.randomUUID();
        private final RecursivePayload self = this;

        public UUID getId() {
            return id;
        }

        public RecursivePayload getSelf() {
            return self;
        }
    }
}
