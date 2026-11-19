package de.hamza.bankdocs_assistant.service;

import de.hamza.bankdocs_assistant.api.AskResponse;
import de.hamza.bankdocs_assistant.rag.DocumentIngestor;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AskService {

    public static final String NO_INFORMATION = "Dazu liegen mir keine Informationen vor.";

    private static final Logger log = LoggerFactory.getLogger(AskService.class);

    private final ChatModel chatModel;
    private final ContentRetriever contentRetriever;

    public AskService(ChatModel chatModel, ContentRetriever contentRetriever) {
        this.chatModel = chatModel;
        this.contentRetriever = contentRetriever;
    }

    public AskResponse ask(String question) {
        List<Content> contents = contentRetriever.retrieve(Query.from(question));
        if (log.isDebugEnabled()) {
            contents.forEach(c -> log.debug("score={} file={} text={}",
                    c.metadata().get(ContentMetadata.SCORE), fileName(c.textSegment()),
                    c.textSegment().text().replaceAll("\\s+", " ")));
        }
        List<TextSegment> segments = contents.stream()
                .map(Content::textSegment)
                .toList();

        // Nothing relevant found: refuse without calling the LLM (no hallucination, no cost).
        if (segments.isEmpty()) {
            return new AskResponse(NO_INFORMATION, List.of());
        }

        String answer = chatModel.chat(buildPrompt(question, segments));

        // The LLM itself decided the sources do not answer the question: do not cite them.
        if (isRefusal(answer)) {
            return new AskResponse(NO_INFORMATION, List.of());
        }

        List<String> sources = segments.stream()
                .map(AskService::fileName)
                .distinct()
                .toList();
        return new AskResponse(answer, sources);
    }

    static String buildPrompt(String question, List<TextSegment> segments) {
        StringBuilder prompt = new StringBuilder()
                .append("Du bist ein Assistent der Musterbank. Beantworte die Frage NUR anhand der Quellen.\n")
                .append("Zitiere jede Aussage mit [1], [2] ...\n")
                .append("Wenn die Quellen die Antwort nicht enthalten, antworte exakt:\n")
                .append('"').append(NO_INFORMATION).append("\"\n\n")
                .append("Quellen:\n");
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            prompt.append('[').append(i + 1).append("] (").append(fileName(segment)).append(") ")
                    .append(segment.text().strip())
                    .append("\n\n");
        }
        return prompt.append("Frage: ").append(question).toString();
    }

    static boolean isRefusal(String answer) {
        return answer != null && answer.strip().replace("\"", "").equals(NO_INFORMATION);
    }

    private static String fileName(TextSegment segment) {
        String name = segment.metadata().getString(DocumentIngestor.FILE_NAME);
        return name != null ? name : "unbekannt";
    }
}
