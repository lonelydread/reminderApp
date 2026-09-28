package ru.reminderapp.nikita.notification;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ru.reminderapp.nikita.reminder.Reminder;
import ru.reminderapp.nikita.reminder.ReminderRepository;

@Service
public class ReminderNotificationService {

    private final ReminderRepository reminderRepository;
    private final EmailNotificationSender emailSender;
    private final TelegramNotificationSender telegramSender;

    public ReminderNotificationService(
            ReminderRepository reminderRepository,
            EmailNotificationSender emailSender,
            TelegramNotificationSender telegramSender) {
        this.reminderRepository = reminderRepository;
        this.emailSender = emailSender;
        this.telegramSender = telegramSender;
    }

    @Transactional(readOnly = true)
    public List<Long> findDueReminderIds(Instant now, int batchSize) {
        return reminderRepository.findDueReminderIds(now, PageRequest.of(0, batchSize));
    }

    @Transactional
    public void process(Long reminderId, Instant now) {
        Reminder reminder = reminderRepository.findByIdWithUser(reminderId).orElse(null);
        if (reminder == null || reminder.getRemind().isAfter(now)) {
            return;
        }

        processEmail(reminder, now);
        processTelegram(reminder, now);
    }

    private void processEmail(Reminder reminder, Instant processedAt) {
        if (reminder.getEmailNotificationProcessedAt() != null) {
            return;
        }

        String email = reminder.getUser().getEmail();
        if (!StringUtils.hasText(email)) {
            reminder.markEmailNotificationProcessed(processedAt);
            return;
        }

        try {
            emailSender.send(reminder);
            reminder.markEmailNotificationProcessed(processedAt);
        }
        catch (RuntimeException ignored) {
        }

    }

    private void processTelegram(Reminder reminder, Instant processedAt) {
        if (reminder.getTelegramNotificationProcessedAt() != null) {
            return;
        }

        if (reminder.getUser().getTelegramChatId() == null) {
            reminder.markTelegramNotificationProcessed(processedAt);
            return;
        }

        try {
            telegramSender.send(reminder);
            reminder.markTelegramNotificationProcessed(processedAt);
        }
        catch (RuntimeException ignored) {
        }
    }
}