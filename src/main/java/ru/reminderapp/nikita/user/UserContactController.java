package ru.reminderapp.nikita.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ru.reminderapp.nikita.user.dto.EmailRequest;
import ru.reminderapp.nikita.user.dto.TelegramChatRequest;

@RestController
@RequestMapping("/api/user")
public class UserContactController {

    private final UserContactService contactService;

    public UserContactController(UserContactService contactService) {
        this.contactService = contactService;
    }

    @PutMapping("/email")
    public void setEmail(
            @AuthenticationPrincipal OidcUser user,
            @Valid @RequestBody EmailRequest request) {
        contactService.setEmail(user.getSubject(), request.email());
    }

    @PutMapping("/telegram")
    public void setTelegramChatId(
            @AuthenticationPrincipal OidcUser user,
            @Valid @RequestBody TelegramChatRequest request) {
        contactService.setTelegramChatId(user.getSubject(), user.getEmail(), request.chatId());
    }
}
