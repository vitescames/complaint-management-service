package com.complaintmanagementservice.infrastructure.scheduler;

import com.complaintmanagementservice.application.port.in.PublishSlaWarningsUseCase;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SlaWarningSchedulerTest {

    @Test
    void shouldDelegateScheduledExecution() {
        PublishSlaWarningsUseCase useCase = mock(PublishSlaWarningsUseCase.class);
        SlaWarningScheduler scheduler = new SlaWarningScheduler(useCase);

        scheduler.publishWarnings();

        verify(useCase).publishWarnings();
    }
}
