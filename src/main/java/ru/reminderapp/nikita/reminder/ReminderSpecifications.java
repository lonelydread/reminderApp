package ru.reminderapp.nikita.reminder;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class ReminderSpecifications {

    public static Specification<Reminder> belongsTo(String oauthSubject) {
        return (root, query, builder) ->
                builder.equal(
                        root.get("user").get("oauthSubject"),
                        oauthSubject);
    }

    public static Specification<Reminder> containsText (String text) {
        return (root, query, builder) -> {
            if (text == null || text.isBlank()) {
                return builder.conjunction();
            }

            String pattern =
                    "%" + text.trim().toLowerCase() + "%";

            return builder.or(
                    builder.like(
                            builder.lower(root.get("title")),
                            pattern),
                    builder.like(
                            builder.lower(root.get("description")),
                            pattern));
        };
    }

    public static Specification<Reminder> remindAt(Instant remindAt) {
        return (root, query, builder) ->
                remindAt == null
                        ? builder.conjunction()
                        : builder.equal(
                        root.get("remind"), remindAt);
    }

    public static Specification<Reminder> remindFrom(Instant from) {
        return (root, query, builder) ->
                from == null
                        ? builder.conjunction()
                        : builder.greaterThanOrEqualTo(
                        root.get("remind"), from);
    }

    public static Specification<Reminder> remindTo(Instant to) {
        return (root, query, builder) ->
                to == null
                        ? builder.conjunction()
                        : builder.lessThanOrEqualTo(
                        root.get("remind"), to);
    }
}
