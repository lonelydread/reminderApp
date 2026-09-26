package ru.reminderapp.nikita.notification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ru.reminderapp.nikita.reminder.Reminder;

@Component
public class EmailNotificationSender {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailNotificationSender(
            JavaMailSender mailSender,
            @Value("${app.notification.email.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(Reminder reminder) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(reminder.getUser().getEmail());
        message.setSubject(reminder.getTitle());
        message.setText(reminder.getDescription() == null ? "" : reminder.getDescription());
        message.setFrom(from);

        mailSender.send(message);
    }
}