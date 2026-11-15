package de.hamza.bankdocs_assistant.service;

import de.hamza.bankdocs_assistant.api.AskResponse;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AskService {

    private final ChatModel chatModel;

    public AskService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public AskResponse ask(String question) {
        String answer = chatModel.chat(question);
        return new AskResponse(answer, List.of());
    }
}
