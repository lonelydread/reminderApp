package ru.reminderapp.nikita.reminder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReminderRepository
        extends JpaRepository<Reminder, Long>, JpaSpecificationExecutor<Reminder> {
}