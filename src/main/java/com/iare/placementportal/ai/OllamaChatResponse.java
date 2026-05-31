package com.iare.placementportal.ai;

public record OllamaChatResponse(
        String model,
        Message message,
        Boolean done
) {
    public record Message(String role, String content) {
    }
}
