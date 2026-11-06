package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.AssistantUnavailableException;
import com.cuymonitor.backend.domain.exception.TooManyAssistantQuestionsException;
import com.cuymonitor.backend.domain.model.assistant.AssistantReply;
import com.cuymonitor.backend.domain.model.assistant.ChatMessage;
import com.cuymonitor.backend.domain.model.assistant.ChatRole;
import com.cuymonitor.backend.domain.port.in.AskAssistantCommand;
import com.cuymonitor.backend.domain.port.in.AskAssistantUseCase;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AssistantController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class AssistantControllerTest {

    private static final UUID ACCOUNT = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");
    private static final String QUESTION = """
            {"language":"es","messages":[{"role":"ASSISTANT","text":"¡Hola! Soy Cuchi."},
             {"role":"USER","text":"¿Qué significa en observación?"}]}""";

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private AskAssistantUseCase askAssistant;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    private static RequestPostProcessor token() {
        return jwt().jwt(j -> j.subject(ACCOUNT.toString()));
    }

    private ResultActions send(String body, boolean withToken) throws Exception {
        var request = post("/api/v1/assistant/messages").contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(withToken ? request.with(token()) : request);
    }

    @Test
    void needsASession() throws Exception {
        send(QUESTION, false).andExpect(status().isUnauthorized());
        verifyNoInteractions(askAssistant);
    }

    @Test
    void asksForTheAccountInTheTokenAndReturnsTheReply() throws Exception {
        given(askAssistant.ask(any())).willReturn(new AssistantReply("Que algo cambió y se está vigilando."));

        send(QUESTION, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Que algo cambió y se está vigilando."));

        ArgumentCaptor<AskAssistantCommand> asked = ArgumentCaptor.forClass(AskAssistantCommand.class);
        verify(askAssistant).ask(asked.capture());
        assertThat(asked.getValue().requesterId()).isEqualTo(ACCOUNT);
        assertThat(asked.getValue().conversation()).extracting(ChatMessage::role)
                .containsExactly(ChatRole.ASSISTANT, ChatRole.USER);
    }

    @Test
    void aBadConversationIsA400() throws Exception {
        send("{\"messages\":[]}", true).andExpect(status().isBadRequest());
        send("{\"messages\":[{\"role\":\"USER\",\"text\":\" \"}]}", true).andExpect(status().isBadRequest());
        send("{\"messages\":[{\"role\":\"SYSTEM\",\"text\":\"hi\"}]}", true).andExpect(status().isBadRequest());
        send("{\"language\":\"fr\",\"messages\":[{\"role\":\"USER\",\"text\":\"hi\"}]}", true)
                .andExpect(status().isBadRequest());
        // the question has to be last
        send("{\"messages\":[{\"role\":\"ASSISTANT\",\"text\":\"hi\"}]}", true)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("bad_request"));
        verifyNoInteractions(askAssistant);
    }

    @Test
    void anUnavailableModelIsA503WithoutDetails() throws Exception {
        given(askAssistant.ask(any())).willThrow(new AssistantUnavailableException("ThrottlingException xyz"));

        send(QUESTION, true)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("service_unavailable"))
                .andExpect(jsonPath("$.message").value("the assistant is not available right now"));
    }

    @Test
    void tooManyQuestionsIsA429() throws Exception {
        given(askAssistant.ask(any())).willThrow(new TooManyAssistantQuestionsException());

        send(QUESTION, true)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("too_many_requests"));
    }
}
