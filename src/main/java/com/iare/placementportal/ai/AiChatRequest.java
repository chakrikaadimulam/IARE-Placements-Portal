package com.iare.placementportal.ai;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank(message = "Message is required.")
        String message
) {
}
