package ru.reminderapp.nikita.notification;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderNotificationScheduler {

    private final ReminderNotificationService notificationService;
    private final int batchSize;

    public ReminderNotificationScheduler(
            ReminderNotificationService notificationService,
            @Value("${app.notification.batch-size:100}") int batchSize) {
        this.notificationService = notificationService;
        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString = "${app.notification.poll-delay-ms:30000}",
            initialDelayString = "${app.notification.initial-delay-ms:5000}")
    public void sendDueNotifications() {
        Instant now = Instant.now();
        List<Long> reminderIds = notificationService.findDueReminderIds(now, batchSize);

        for (Long reminderId : reminderIds) {
            notificationService.process(reminderId, now);
        }
    }
}