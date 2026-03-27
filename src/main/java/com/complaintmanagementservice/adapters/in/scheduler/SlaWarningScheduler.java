package com.complaintmanagementservice.adapters.in.scheduler;

import com.complaintmanagementservice.application.port.in.PublishSlaWarningsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
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
