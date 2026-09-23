package ru.reminderapp.nikita.reminder.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReminderRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 4096) String description,
        @NotNull Instant remind) {
}