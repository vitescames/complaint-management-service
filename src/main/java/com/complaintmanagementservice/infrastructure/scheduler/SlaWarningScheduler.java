package com.complaintmanagementservice.infrastructure.scheduler;

import com.complaintmanagementservice.application.port.in.PublishSlaWarningsUseCase;
import org.springframework.scheduling.annotation.Scheduled;

public class SlaWarningScheduler {

    private final PublishSlaWarningsUseCase publishSlaWarningsUseCase;

    public SlaWarningScheduler(PublishSlaWarningsUseCase publishSlaWarningsUseCase) {
        this.publishSlaWarningsUseCase = publishSlaWarningsUseCase;
    }

    @Scheduled(cron = "${application.scheduler.sla-warning-cron}")
    public void publishWarnings() {
        publishSlaWarningsUseCase.publishWarnings();
    }
}
