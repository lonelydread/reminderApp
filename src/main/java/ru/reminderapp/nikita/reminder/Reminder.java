package ru.reminderapp.nikita.reminder;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.reminderapp.nikita.user.User;

@Entity
@Table(name = "reminder")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, length = 255)
    private String title;

    @Setter
    @Column(length = 4096)
    private String description;

    @Setter
    @Column(nullable = false)
    private Instant remind;

    @Column(name = "email_notification_processed_at")
    private Instant emailNotificationProcessedAt;

    @Column(name = "telegram_notification_processed_at")
    private Instant telegramNotificationProcessedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reminder_user"))
    private User user;

    public Reminder(User user, String title, String description, Instant remind) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.remind = remind;
    }

    public void resetNotificationProcessing() {
        this.emailNotificationProcessedAt = null;
        this.telegramNotificationProcessedAt = null;
    }

    public void markEmailNotificationProcessed(Instant processedAt) {
        this.emailNotificationProcessedAt = processedAt;
    }

    public void markTelegramNotificationProcessed(Instant processedAt) {
        this.telegramNotificationProcessedAt = processedAt;
    }
}