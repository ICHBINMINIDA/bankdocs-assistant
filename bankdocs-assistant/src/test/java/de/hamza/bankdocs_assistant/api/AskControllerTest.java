package de.hamza.bankdocs_assistant.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.hamza.bankdocs_assistant.service.AskService;
import dev.langchain4j.exception.InternalServerException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AskController.class)
class AskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AskService askService;

    @Test
    void validQuestionReturns200WithAnswer() throws Exception {
        when(askService.ask(anyString()))
                .thenReturn(new AskResponse("Testantwort", List.of()));

        mockMvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Was ist ein Girokonto?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").exists());
    }

    @Test
    void blankQuestionReturns400() throws Exception {
        mockMvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void llmBackendDownReturns503WithoutInternalDetails() throws Exception {
        when(askService.ask(anyString()))
                .thenThrow(new InternalServerException("Cannot connect to host 10.0.0.1:18080"));

        mockMvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Was kostet die Girocard?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("LLM nicht erreichbar"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("18080"))));
    }
}
