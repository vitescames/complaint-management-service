package com.complaintmanagementservice.adapters.out.config;

import com.complaintmanagementservice.adapters.out.messaging.JacksonTextMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.IndividualDeadLetterStrategy;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConverter;

import java.util.List;

@Configuration
@EnableJms
public class MessagingConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public BrokerService embeddedBroker(MessagingProperties messagingProperties) throws Exception {
        BrokerService brokerService = new BrokerService();
        brokerService.setBrokerName(messagingProperties.brokerName());
        brokerService.setPersistent(false);
        brokerService.setUseJmx(false);
        brokerService.setSchedulerSupport(false);
        brokerService.setDestinationPolicy(destinationPolicy());
        brokerService.addConnector("vm://" + messagingProperties.brokerName());
        return brokerService;
    }

    @Bean
    public ConnectionFactory connectionFactory(MessagingProperties messagingProperties) {
        ActiveMQConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory("vm://" + messagingProperties.brokerName() + "?create=false");
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(messagingProperties.redelivery().maximumRedeliveries());
        redeliveryPolicy.setInitialRedeliveryDelay(messagingProperties.redelivery().initialDelayMillis());
        redeliveryPolicy.setUseExponentialBackOff(true);
        redeliveryPolicy.setBackOffMultiplier(messagingProperties.redelivery().backoffMultiplier());
        connectionFactory.setRedeliveryPolicy(redeliveryPolicy);
        connectionFactory.setTrustAllPackages(false);
        connectionFactory.setTrustedPackages(List.of(
                "com.complaintmanagementservice.adapters.in.messaging.dto",
                "com.complaintmanagementservice.adapters.out.messaging.dto"
        ));
        return connectionFactory;
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new JacksonTextMessageConverter(objectMapper);
    }

    @Bean
    public DefaultJmsListenerContainerFactory complaintListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setSessionTransacted(true);
        factory.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
        factory.setErrorHandler(error -> {
            throw new IllegalStateException("Falha ao consumir a mensagem da fila de reclamações.", error);
        });
        return factory;
    }

    private PolicyMap destinationPolicy() {
        IndividualDeadLetterStrategy deadLetterStrategy = new IndividualDeadLetterStrategy();
        deadLetterStrategy.setUseQueueForQueueMessages(true);
        deadLetterStrategy.setQueuePrefix("DLQ.");
        PolicyEntry policyEntry = new PolicyEntry();
        policyEntry.setQueue(">");
        policyEntry.setDeadLetterStrategy(deadLetterStrategy);
        PolicyMap policyMap = new PolicyMap();
        policyMap.setDefaultEntry(policyEntry);
        return policyMap;
    }
}
