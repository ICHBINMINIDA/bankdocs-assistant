package de.hamza.bankdocs_assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hamza.bankdocs_assistant.api.AskResponse;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Pure unit test: Mockito only, no Spring context, no real LLM. */
class AskServiceTest {

    private final ChatModel chatModel = mock(ChatModel.class);
    private final ContentRetriever retriever = mock(ContentRetriever.class);
    private final AskService service = new AskService(chatModel, retriever);

    private static Content segment(String text, String file) {
        return Content.from(TextSegment.from(text, Metadata.from("file_name", file)));
    }

    @Test
    void emptyRetrievalReturnsRefusalAndNeverCallsLlm() {
        when(retriever.retrieve(any(Query.class))).thenReturn(List.of());

        AskResponse response = service.ask("Wie wird das Wetter morgen?");

        assertThat(response.answer()).isEqualTo(AskService.NO_INFORMATION);
        assertThat(response.sources()).isEmpty();
        verify(chatModel, never()).chat(anyString());
    }

    @Test
    void twoSegmentsEndUpInPromptAndInSources() {
        when(retriever.retrieve(any(Query.class))).thenReturn(List.of(
                segment("Im Kontomodell MusterGiro Online kostet die Girocard 9,00 € pro Jahr.",
                        "preisverzeichnis.md"),
                segment("Der Kunde kann das Girokonto jederzeit ohne Einhaltung einer Frist kündigen.",
                        "girokonto-bedingungen.md")));
        when(chatModel.chat(anyString())).thenReturn("Die Girocard kostet 9,00 € pro Jahr [1].");

        AskResponse response = service.ask("Was kostet die Girocard?");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(chatModel).chat(prompt.capture());
        assertThat(prompt.getValue())
                .contains("NUR anhand der Quellen")
                .contains("[1] (preisverzeichnis.md) Im Kontomodell MusterGiro Online kostet die Girocard 9,00 € pro Jahr.")
                .contains("[2] (girokonto-bedingungen.md) Der Kunde kann das Girokonto jederzeit ohne Einhaltung einer Frist kündigen.")
                .endsWith("Frage: Was kostet die Girocard?");
        assertThat(response.answer()).isEqualTo("Die Girocard kostet 9,00 € pro Jahr [1].");
        assertThat(response.sources()).containsExactly("preisverzeichnis.md", "girokonto-bedingungen.md");
    }

    @Test
    void sourcesAreDistinctWhenTwoSegmentsComeFromTheSameFile() {
        when(retriever.retrieve(any(Query.class))).thenReturn(List.of(
                segment("Girocard: 9,00 €", "preisverzeichnis.md"),
                segment("Ersatzkarte: 10,00 €", "preisverzeichnis.md")));
        when(chatModel.chat(anyString())).thenReturn("Antwort [1][2]");

        assertThat(service.ask("Kartenpreise?").sources()).containsExactly("preisverzeichnis.md");
    }

    @Test
    void llmRefusalReturnsNoSources() {
        when(retriever.retrieve(any(Query.class))).thenReturn(List.of(
                segment("Kündigung jederzeit ohne Frist", "girokonto-bedingungen.md")));
        when(chatModel.chat(anyString())).thenReturn("\"Dazu liegen mir keine Informationen vor.\"");

        AskResponse response = service.ask("Wie wird das Wetter morgen?");

        assertThat(response.answer()).isEqualTo(AskService.NO_INFORMATION);
        assertThat(response.sources()).isEmpty();
    }
}
