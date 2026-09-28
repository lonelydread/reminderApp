package ru.reminderapp.nikita.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TelegramChatRequest(
        @NotNull @Positive Long chatId) {
}
