package ru.reminderapp.nikita.reminder;

import java.security.Principal;
import java.time.Instant;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import ru.reminderapp.nikita.reminder.dto.PageResponse;
import ru.reminderapp.nikita.reminder.dto.ReminderRequest;
import ru.reminderapp.nikita.reminder.dto.ReminderResponse;

@RestController
@RequestMapping("/api")
@Validated
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping("/reminder/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse create(
            Principal principal,
            @Valid @RequestBody ReminderRequest request) {
        return reminderService.create(principal.getName(), request);
    }

    @PutMapping("/reminder/{id}")
    public ReminderResponse update(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReminderRequest request) {
        return reminderService.update(principal.getName(), id, request);
    }

    @DeleteMapping("/reminder/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @RequestParam Long id) {
        reminderService.delete(principal.getName(), id);
    }

    @GetMapping({"/search", "/list", "/sort", "/filtr"})
    public PageResponse<ReminderResponse> findAll(
            Principal principal,
            @RequestParam(required = false) String query,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant remindAt,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "remind") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(reminderService.findAll(
                principal.getName(),
                query,
                remindAt,
                from,
                to,
                sortBy,
                direction,
                page,
                size));
    }
}