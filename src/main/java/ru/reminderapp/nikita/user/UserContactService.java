package ru.reminderapp.nikita.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserContactService {

    private final UserRepository userRepository;

    public UserContactService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void setEmail(String oauthSubject, String email) {
        User user = userRepository.findByOauthSubject(oauthSubject)
                .orElseGet(() -> new User(oauthSubject, email));
        user.setEmail(email);
        userRepository.save(user);
    }

    @Transactional
    public void setTelegramChatId(String oauthSubject, String googleEmail, Long chatId) {
        User user = userRepository.findByOauthSubject(oauthSubject)
                .orElseGet(() -> new User(oauthSubject, requiredGoogleEmail(googleEmail)));
        user.setTelegramChatId(chatId);
        userRepository.save(user);
    }

    private String requiredGoogleEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "google account email is unavailable");
        }
        return email;
    }
}
