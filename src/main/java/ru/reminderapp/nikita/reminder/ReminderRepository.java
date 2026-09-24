package ru.reminderapp.nikita.reminder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderRepository
        extends JpaRepository<Reminder, Long>, JpaSpecificationExecutor<Reminder> {

    @Query("""
            select reminder.id
            from Reminder reminder
            where reminder.remind <= :now
              and (reminder.emailNotificationProcessedAt is null
                   or reminder.telegramNotificationProcessedAt is null)
            order by reminder.remind, reminder.id
            """)
    List<Long> findDueReminderIds(@Param("now") Instant now, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("select reminder from Reminder reminder where reminder.id = :id")
    Optional<Reminder> findByIdWithUser(@Param("id") Long id);
}