package de.hamza.bankdocs_assistant.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/** Checks loading, splitting and filtering only; embedding needs a real backend and is not tested here. */
class DocumentIngestorTest {

    private static final int CHUNK_SIZE = 1000;
    private static final int MIN_LENGTH = 40;

    private final DocumentIngestor ingestor = new DocumentIngestor(
            mock(EmbeddingModel.class), new InMemoryEmbeddingStore<>(), CHUNK_SIZE, 100, MIN_LENGTH);

    @Test
    void loadsAllDocsWithFileNameMetadata() throws Exception {
        List<Document> docs = ingestor.loadDocuments();

        assertThat(docs).extracting(d -> d.metadata().getString(DocumentIngestor.FILE_NAME))
                .contains("preisverzeichnis.md", "girokonto-bedingungen.md",
                        "kreditkarte-faq.md", "zahlungskontengesetz-zkg.md");
    }

    @Test
    void everyKeptSegmentHasFileNameRespectsMaxSizeAndIsNotTooShort() throws Exception {
        List<TextSegment> segments = ingestor.splitter().splitAll(ingestor.loadDocuments()).stream()
                .map(ingestor::dropTooShort)
                .filter(Objects::nonNull)
                .toList();

        assertThat(segments).isNotEmpty().allSatisfy(s -> {
            assertThat(s.metadata().getString(DocumentIngestor.FILE_NAME)).endsWith(".md");
            assertThat(s.text().length()).isLessThanOrEqualTo(CHUNK_SIZE);
            assertThat(s.text().strip().length()).isGreaterThanOrEqualTo(MIN_LENGTH);
        });
    }

    @Test
    void dropsSplitterLeftovers() {
        assertThat(ingestor.dropTooShort(TextSegment.from("Übertragungswege."))).isNull();
    }
}
