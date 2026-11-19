package de.hamza.bankdocs_assistant.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    @Bean
    public ChatModel chatModel(
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.model}") String modelName,
            @Value("${llm.temperature}") double temperature) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * Embedding model behind the same LiteLLM gateway (alias "embed-default" -> ollama/bge-m3).
     * Switching provider later (e.g. Azure) only changes configuration, not this code.
     */
    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.embedding-model}") String modelName) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
