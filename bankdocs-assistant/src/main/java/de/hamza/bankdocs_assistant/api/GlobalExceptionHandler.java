package de.hamza.bankdocs_assistant.api;

import dev.langchain4j.exception.LangChain4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns failures of the LLM / embedding backend (LiteLLM down, Ollama unreachable, timeout)
 * into a clear 503 instead of a generic 500. Internal details stay in the log, not in the response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(LangChain4jException.class)
    public ProblemDetail handleLlmUnavailable(LangChain4jException e) {
        log.error("LLM backend unavailable: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Der Sprachdienst ist derzeit nicht erreichbar. Bitte versuchen Sie es später erneut.");
        problem.setTitle("LLM nicht erreichbar");
        return problem;
    }
}
