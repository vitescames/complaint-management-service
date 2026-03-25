package com.complaintmanagementservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class ComplaintManagementServiceApplicationTest {

    @Test
    void shouldRunAndCloseApplication() {
        new ComplaintManagementServiceApplication();
        ComplaintManagementServiceApplication.closeApplication();

        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        String[] args = {"--test"};
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.run(ComplaintManagementServiceApplication.class, args))
                    .thenReturn(applicationContext);
            ComplaintManagementServiceApplication.main(args);
            springApplication.verify(() -> SpringApplication.run(ComplaintManagementServiceApplication.class, args));
        }

        ComplaintManagementServiceApplication.closeApplication();
        ComplaintManagementServiceApplication.closeApplication();

        verify(applicationContext).close();
    }
}
