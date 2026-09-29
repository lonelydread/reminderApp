package ru.reminderapp.nikita.reminder.dto;

import java.time.Instant;

import ru.reminderapp.nikita.reminder.Reminder;

public record ReminderResponse(
        Long id,
        String title,
        String description,
        Instant remind) {

    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getRemind());
    }
}