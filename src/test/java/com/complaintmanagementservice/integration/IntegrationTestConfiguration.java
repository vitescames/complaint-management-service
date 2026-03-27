package com.complaintmanagementservice.integration;

import com.complaintmanagementservice.TestFixtures;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfiguration {

    @Bean
    @Primary
    public Clock integrationTestClock() {
        return TestFixtures.FIXED_CLOCK;
    }
}
