package de.hamza.bankdocs_assistant.api;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(@NotBlank String question) {
}
