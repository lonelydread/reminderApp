package ru.reminderapp.nikita.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByOauthSubject(String oauthSubject);
}