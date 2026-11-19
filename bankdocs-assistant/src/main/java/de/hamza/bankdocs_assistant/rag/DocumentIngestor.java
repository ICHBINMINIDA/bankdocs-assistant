package de.hamza.bankdocs_assistant.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Loads all Markdown files from classpath:docs/ at startup, splits them into overlapping
 * segments, embeds them and stores them in the embedding store.
 *
 * <p>Reads from the classpath (not the file system) so it also works inside the Docker jar.
 * Disable with rag.ingest-on-startup=false (e.g. in tests without an embedding backend).
 */
@Component
@ConditionalOnProperty(name = "rag.ingest-on-startup", havingValue = "true", matchIfMissing = true)
public class DocumentIngestor implements ApplicationRunner {

    /** Metadata key holding the source file name of every segment. */
    public static final String FILE_NAME = Document.FILE_NAME;

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestor.class);
    private static final String DOCS_PATTERN = "classpath:docs/*.md";

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final int chunkSize;
    private final int chunkOverlap;
    private final int minSegmentLength;

    public DocumentIngestor(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            @Value("${rag.chunk-size}") int chunkSize,
            @Value("${rag.chunk-overlap}") int chunkOverlap,
            @Value("${rag.min-segment-length}") int minSegmentLength) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.minSegmentLength = minSegmentLength;
    }

    DocumentSplitter splitter() {
        return DocumentSplitters.recursive(chunkSize, chunkOverlap);
    }

    /** Returns null for splitter leftovers that are too short to carry information; the ingestor drops them. */
    TextSegment dropTooShort(TextSegment segment) {
        return segment.text().strip().length() < minSegmentLength ? null : segment;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        List<Document> documents = loadDocuments();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter())
                .textSegmentTransformer(this::dropTooShort)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        long start = System.currentTimeMillis();
        ingestor.ingest(documents);
        log.info("Ingested {} documents into the embedding store in {} ms",
                documents.size(), System.currentTimeMillis() - start);
    }

    List<Document> loadDocuments() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(DOCS_PATTERN);
        List<Document> documents = new ArrayList<>();
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            String text = resource.getContentAsString(StandardCharsets.UTF_8);
            // Segments created by the splitter inherit this metadata, so every chunk knows its source.
            documents.add(Document.from(text, Metadata.from(FILE_NAME, fileName)));
            log.info("Loaded {} ({} chars)", fileName, text.length());
        }
        if (documents.isEmpty()) {
            throw new IllegalStateException("No documents found under " + DOCS_PATTERN);
        }
        return documents;
    }
}
