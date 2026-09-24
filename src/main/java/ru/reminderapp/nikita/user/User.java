package ru.reminderapp.nikita.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oauth_subject", nullable = false, unique = true, length = 255)
    private String oauthSubject;

    @Column(length = 320)
    @Setter
    private String email;

    @Column(name = "telegram_chat_id")
    @Setter
    private Long telegramChatId;

    public User(String oauthSubject) {
        this.oauthSubject = oauthSubject;
    }
}