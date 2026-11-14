package de.hamza.bankdocs_assistant.api;

import java.util.List;

public record AskResponse(String answer, List<String> sources) {
}
