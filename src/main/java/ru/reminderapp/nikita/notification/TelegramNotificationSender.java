package ru.reminderapp.nikita.notification;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import ru.reminderapp.nikita.reminder.Reminder;

@Component
public class TelegramNotificationSender {

    private final int MAX_MESSAGE_LENGTH;
    private final RestClient restClient;
    private final String botToken;

    public TelegramNotificationSender(
            RestClient.Builder restClientBuilder,
            @Value("${app.notification.telegram.api-url:https://api.telegram.org}") String apiUrl,
            @Value("${app.notification.telegram.bot-token:}") String botToken,
            @Value("${app.notification.telegram.max-message-length:4096}") int length) {
        this.restClient = restClientBuilder.baseUrl(apiUrl).build();
        this.botToken = botToken;
        this.MAX_MESSAGE_LENGTH = length;
    }

    public void send(Reminder reminder) {
        if (!StringUtils.hasText(botToken)) {
            throw new IllegalStateException("telegram bot token is not configured");
        }

        String text = buildMessage(reminder);
        restClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .body(Map.of(
                        "chat_id", reminder.getUser().getTelegramChatId(),
                        "text", text))
                .retrieve()
                .toBodilessEntity();
    }

    private String buildMessage(Reminder reminder) {
        String description = reminder.getDescription();
        String text = StringUtils.hasText(description)
                ? reminder.getTitle() + System.lineSeparator() + description
                : reminder.getTitle();

        return text.length() <= MAX_MESSAGE_LENGTH
                ? text
                : text.substring(0, MAX_MESSAGE_LENGTH);
    }
}