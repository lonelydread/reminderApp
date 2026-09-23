package ru.reminderapp.nikita.reminder.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> content,
        long total,
        int pages,
        int current,
        int size) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }
}