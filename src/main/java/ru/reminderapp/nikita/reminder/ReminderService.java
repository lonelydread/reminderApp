package ru.reminderapp.nikita.reminder;

import java.time.Instant;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.Predicate;
import ru.reminderapp.nikita.reminder.dto.ReminderRequest;
import ru.reminderapp.nikita.reminder.dto.ReminderResponse;
import ru.reminderapp.nikita.user.User;
import ru.reminderapp.nikita.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;

    public ReminderService(ReminderRepository reminderRepository, UserRepository userRepository) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReminderResponse create(String oauthSubject, ReminderRequest request) {
        User user = userRepository.findByOauthSubject(oauthSubject)
                .orElseGet(() -> userRepository.save(new User(oauthSubject)));

        Reminder reminder = new Reminder(
                user,
                request.title(),
                request.description(),
                request.remind());
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @Transactional
    public ReminderResponse update(String oauthSubject, Long id, ReminderRequest request) {
        Reminder reminder = findOwnedReminder(oauthSubject, id);
        boolean scheduleChanged = !reminder.getRemind().equals(request.remind());

        reminder.setTitle(request.title());
        reminder.setDescription(request.description());
        reminder.setRemind(request.remind());

        if (scheduleChanged) {
            reminder.resetNotificationProcessing();
        }
        return ReminderResponse.from(reminder);
    }

    @Transactional
    public void delete(String oauthSubject, Long id) {
        reminderRepository.delete(findOwnedReminder(oauthSubject, id));
    }

    public Page<ReminderResponse> findAll(
            String oauthSubject,
            String query,
            Instant remindAt,
            Instant from,
            Instant to,
            String sortBy,
            Sort.Direction direction,
            int page,
            int size) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "'from' must be before or equal 'to'");
        }

        String sortProperty = switch (sortBy.toLowerCase()) {
            case "title" -> "title";
            case "description" -> "description";
            case "date", "time", "remind" -> "remind";
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "sortBy must be name, title, date, time or remind");
        };

        Specification<Reminder> specification =
                ReminderSpecifications.belongsTo(oauthSubject)
                        .and(ReminderSpecifications.containsText(query))
                        .and(ReminderSpecifications.remindAt(remindAt))
                        .and(ReminderSpecifications.remindFrom(from))
                        .and(ReminderSpecifications.remindTo(to));

        PageRequest pageRequest = PageRequest.of(
                page, size, Sort.by(direction, sortProperty).and(Sort.by("id")));
        return reminderRepository.findAll(specification, pageRequest)
                .map(ReminderResponse::from);
    }

    private Reminder findOwnedReminder(String oauthSubject, Long id) {
        return reminderRepository.findById(id)
                .filter(reminder -> reminder.getUser().getOauthSubject().equals(oauthSubject))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "reminder not found"));
    }
}