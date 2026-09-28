package ru.reminderapp.nikita.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.containsString;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

import ru.reminderapp.nikita.notification.ReminderNotificationScheduler;
import ru.reminderapp.nikita.user.UserRepository;

@SpringBootTest(properties = {"spring.security.oauth2.client.registration.google.client-id=test-client",
                "spring.security.oauth2.client.registration.google.client-secret=test-secret",
                "MAIL_USERNAME=test@example.com", "MAIL_PASSWORD=test-only",
                "TELEGRAM_BOT_TOKEN=test-only"})
@AutoConfigureMockMvc
class ReminderApiIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReminderRepository reminders;

    @Autowired
    private UserRepository users;


    @MockitoBean
    private ReminderNotificationScheduler scheduler;

    @BeforeEach
    void cleanDatabase() {
        reminders.deleteAll();
        users.deleteAll();
    }

    @Test
    void createPersistsRemindersAndReusesOauthUser() throws Exception {
        long firstId = create("alice", "Pay bill", "Electricity", "2030-01-02T10:00:00Z");
        long secondId = create("alice", "Visit doctor", "Annual check", "2030-01-03T10:00:00Z");

        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByOauthSubject("alice").orElseThrow().getEmail())
                .isEqualTo("alice@example.com");
        assertThat(reminders.count()).isEqualTo(2);
        assertThat(reminders.findById(firstId).orElseThrow().getTitle()).isEqualTo("Pay bill");
    }

    @Test
    void updatePersistsChangesAndResetsNotificationProgressWhenScheduleChanges() throws Exception {
        long id = create("alice", "Old title", "Old description", "2030-01-02T10:00:00Z");
        var reminder = reminders.findById(id).orElseThrow();
        reminder.markEmailNotificationProcessed(Instant.parse("2029-01-01T00:00:00Z"));
        reminder.markTelegramNotificationProcessed(Instant.parse("2029-01-01T00:00:00Z"));
        reminders.save(reminder);

        mvc.perform(put("/api/reminder/{id}", id)
                        .with(oidcLogin().idToken(token -> token.subject("bob")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Stolen", "Hidden", "2030-01-04T10:00:00Z")))
                .andExpect(status().isNotFound());
        assertThat(reminders.findById(id).orElseThrow().getTitle()).isEqualTo("Old title");

        mvc.perform(put("/api/reminder/{id}", id)
                        .with(oidcLogin().idToken(token -> token.subject("alice")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("New title", "New description", "2030-01-04T10:00:00Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.description").value("New description"))
                .andExpect(jsonPath("$.remind").value("2030-01-04T10:00:00Z"));

        var updated = reminders.findById(id).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("New title");
        assertThat(updated.getEmailNotificationProcessedAt()).isNull();
        assertThat(updated.getTelegramNotificationProcessedAt()).isNull();
    }

    @Test
    void deleteRequiresOwnershipAndRemovesOwnReminder() throws Exception {
        long id = create("alice", "Pay bill", "Electricity", "2030-01-02T10:00:00Z");

        mvc.perform(delete("/api/reminder/delete")
                        .param("id", Long.toString(id))
                        .with(oidcLogin().idToken(token -> token.subject("bob")))
                        .with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(reminders.existsById(id)).isTrue();

        mvc.perform(delete("/api/reminder/delete")
                        .param("id", Long.toString(id))
                        .with(oidcLogin().idToken(token -> token.subject("alice")))
                        .with(csrf()))
                .andExpect(status().isOk());
        assertThat(reminders.existsById(id)).isFalse();
    }

    @Test
    void listSearchSortAndFilterReturnOnlyCurrentUsersReminders() throws Exception {
        create("alice", "Alpha payment", "Rent", "2030-01-01T10:00:00Z");
        create("alice", "Beta meeting", "Work", "2030-01-02T10:00:00Z");
        create("alice", "Gamma payment", "Electricity", "2030-01-03T10:00:00Z");
        create("bob", "Private payment", "Hidden", "2030-01-04T10:00:00Z");

        mvc.perform(get("/api/list").with(oidcLogin().idToken(token -> token.subject("alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Alpha payment"));

        mvc.perform(get("/api/list").with(oidcLogin().idToken(token -> token.subject("bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Private payment"));

        mvc.perform(get("/api/search")
                        .with(oidcLogin().idToken(token -> token.subject("alice")))
                        .param("query", "PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        mvc.perform(get("/api/sort")
                        .with(oidcLogin().idToken(token -> token.subject("alice")))
                        .param("sortBy", "title")
                        .param("direction", "DESC")
                        .param("size", "2")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.pages").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Gamma payment"));

        mvc.perform(get("/api/filter")
                        .with(oidcLogin().idToken(token -> token.subject("alice")))
                        .param("from", "2030-01-02T00:00:00Z")
                        .param("to", "2030-01-03T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Beta meeting"));

        mvc.perform(get("/api/filter")
                        .with(oidcLogin().idToken(token -> token.subject("alice")))
                        .param("remindAt", "2030-01-03T10:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Gamma payment"));
    }

    @Test
    void rejectsInvalidInputAndRequestsWithoutRequiredSecurity() throws Exception {
        mvc.perform(get("/api/list"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/api/reminder/create")
                        .with(oidcLogin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Pay bill", "Electricity", "2030-01-02T10:00:00Z")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/reminder/create")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","remind":null}
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/filter")
                        .with(oidcLogin())
                        .param("from", "2030-01-03T00:00:00Z")
                        .param("to", "2030-01-02T00:00:00Z"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/sort")
                        .with(oidcLogin())
                        .param("sortBy", "unknown"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signedInUserCanRequestCsrfToken() throws Exception {
        mvc.perform(get("/api/csrf")
                        .with(oidcLogin().idToken(token -> token.subject("alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
    @Test
    void redirectsLoginToGoogle() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("accounts.google.com")));
    }

    @Test
    void contactsCanBeAddedBeforeFirstReminder() throws Exception {
        mvc.perform(put("/api/user/email")
                        .with(oidcLogin().idToken(token -> token.subject("alice")
                                .claim("email", "alice@google.example")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@custom.example\"}"))
                .andExpect(status().isOk());

        mvc.perform(put("/api/user/telegram")
                        .with(oidcLogin().idToken(token -> token.subject("alice")
                                .claim("email", "alice@google.example")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatId\":123456789}"))
                .andExpect(status().isOk());

        assertThat(users.count()).isEqualTo(1);
        var alice = users.findByOauthSubject("alice").orElseThrow();
        assertThat(alice.getEmail()).isEqualTo("alice@custom.example");
        assertThat(alice.getTelegramChatId()).isEqualTo(123456789L);

        create("alice", "Pay bill", "Electricity", "2030-01-02T10:00:00Z");
        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByOauthSubject("alice").orElseThrow().getEmail())
                .isEqualTo("alice@custom.example");

        mvc.perform(put("/api/user/email")
                        .with(oidcLogin().idToken(token -> token.subject("bob")
                                .claim("email", "bob@google.example")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bob@custom.example\"}"))
                .andExpect(status().isOk());
        assertThat(users.findByOauthSubject("alice").orElseThrow().getEmail())
                .isEqualTo("alice@custom.example");
        assertThat(users.findByOauthSubject("bob").orElseThrow().getEmail())
                .isEqualTo("bob@custom.example");
    }

    @Test
    void telegramContactCanBeAddedBeforeFirstReminder() throws Exception {
        mvc.perform(put("/api/user/telegram")
                        .with(oidcLogin().idToken(token -> token.subject("alice")
                                .claim("email", "alice@google.example")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatId\":123456789}"))
                .andExpect(status().isOk());

        var alice = users.findByOauthSubject("alice").orElseThrow();
        assertThat(alice.getEmail()).isEqualTo("alice@google.example");
        assertThat(alice.getTelegramChatId()).isEqualTo(123456789L);
    }

    @Test
    void contactUpdatesRequireCsrfAndValidValues() throws Exception {
        mvc.perform(put("/api/user/email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\"}"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(put("/api/user/email")
                        .with(oidcLogin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/user/email")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/user/telegram")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatId\":-1}"))
                .andExpect(status().isBadRequest());

        assertThat(users.count()).isZero();
    }

    private long create(String subject, String title, String description, String remind)
            throws Exception {
        MvcResult result = mvc.perform(post("/api/reminder/create")
                        .with(oidcLogin().idToken(token -> token.subject(subject)
                                .claim("email", subject + "@example.com")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(title, description, remind)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title))
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private String body(String title, String description, String remind) {
        return """
                {"title":"%s","description":"%s","remind":"%s"}
                """.formatted(title, description, remind);
    }
}
