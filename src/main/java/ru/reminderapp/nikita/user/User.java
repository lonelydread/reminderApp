package ru.reminderapp.nikita.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor()
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oauth_subject", nullable = false, unique = true, length = 255)
    private String oauthSubject;

    @Column(length = 320)
    @Email
    @Setter
    private String email;

    @Column(name = "telegram_chat_id")
    @Setter
    private Long telegramChatId;

    public User(String oauthSubject, String email) {
        this.oauthSubject = oauthSubject;
        this.email = email;
    }
}