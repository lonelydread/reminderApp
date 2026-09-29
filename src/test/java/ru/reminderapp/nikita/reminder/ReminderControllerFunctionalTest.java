package ru.reminderapp.nikita.reminder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.reminderapp.nikita.reminder.dto.ReminderRequest;
import ru.reminderapp.nikita.reminder.dto.ReminderResponse;
import ru.reminderapp.nikita.security.CsrfController;
import ru.reminderapp.nikita.security.SecurityConfig;

@WebMvcTest(value = {ReminderController.class, CsrfController.class},
        properties = {"spring.security.oauth2.client.registration.google.client-id=test-client",
                "spring.security.oauth2.client.registration.google.client-secret=test-secret"})
@Import(SecurityConfig.class)
class ReminderControllerFunctionalTest {

    private static final Instant REMIND = Instant.parse("2030-01-02T10:00:00Z");
    private static final String REQUEST = """
            {"title":"Pay bill","description":"Electricity","remind":"2030-01-02T10:00:00Z"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReminderService service;


    @Test
    void createPassesGoogleSubjectEmailAndBodyToService() throws Exception {
        when(service.create(any(), any(), any())).thenReturn(
                new ReminderResponse(7L, "Pay bill", "Electricity", REMIND));

        mvc.perform(post("/api/reminder/create")
                        .with(oidcLogin().idToken(token -> token.subject("alice")
                                .claim("email", "alice@example.com")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Pay bill"))
                .andExpect(jsonPath("$.remind").value("2030-01-02T10:00:00Z"));

        verify(service).create("alice", "alice@example.com",
                new ReminderRequest("Pay bill", "Electricity", REMIND));
    }

    @Test
    void updatePassesPathIdAndGoogleSubjectToService() throws Exception {
        when(service.update(any(), any(), any())).thenReturn(
                new ReminderResponse(7L, "Pay bill", "Electricity", REMIND));

        mvc.perform(put("/api/reminder/7")
                        .with(oidcLogin().idToken(token -> token.subject("alice")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(service).update("alice", 7L,
                new ReminderRequest("Pay bill", "Electricity", REMIND));
    }

    @Test
    void deletePassesIdAndGoogleSubjectToService() throws Exception {
        mvc.perform(delete("/api/reminder/delete")
                        .param("id", "7")
                        .with(oidcLogin().idToken(token -> token.subject("alice")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(service).delete("alice", 7L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/list", "/api/search", "/api/sort", "/api/filter"})
    void listRoutesReturnPageContract(String route) throws Exception {
        var response = new ReminderResponse(7L, "Pay bill", "Electricity", REMIND);
        when(service.findAll("alice", null, null, null, null, "remind",
                org.springframework.data.domain.Sort.Direction.ASC, 0, 20))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mvc.perform(get(route).with(oidcLogin().idToken(token -> token.subject("alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.pages").value(1))
                .andExpect(jsonPath("$.current").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void searchAndFilterParametersReachService() throws Exception {
        when(service.findAll("alice", "bill", REMIND,
                Instant.parse("2030-01-01T00:00:00Z"),
                Instant.parse("2030-01-03T00:00:00Z"),
                "title", org.springframework.data.domain.Sort.Direction.DESC, 1, 5))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mvc.perform(get("/api/filter")
                        .with(oidcLogin().idToken(token -> token.subject("alice")))
                        .param("query", "bill")
                        .param("remindAt", "2030-01-02T10:00:00Z")
                        .param("from", "2030-01-01T00:00:00Z")
                        .param("to", "2030-01-03T00:00:00Z")
                        .param("sortBy", "title")
                        .param("direction", "DESC")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void invalidCreateBodyIsRejected() throws Exception {
        mvc.perform(post("/api/reminder/create")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","remind":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRequestRedirectsToLoginAndPostRequiresCsrf() throws Exception {
        mvc.perform(get("/api/list"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/api/reminder/create")
                        .with(oidcLogin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isForbidden());
    }
    @Test
    void authenticatedClientCanObtainCsrfToken() throws Exception {
        mvc.perform(get("/api/csrf").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
