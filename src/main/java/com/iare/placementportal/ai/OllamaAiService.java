package com.iare.placementportal.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class OllamaAiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OllamaAiService.class);
    private static final String SYSTEM_PROMPT = "You are IARE AI Placement Assistant. Answer clearly and briefly. For normal general questions, answer directly. For placement questions, give practical student-focused guidance. Keep answers under 6 lines unless the student asks for detailed explanation.";
    private static final String FALLBACK_MESSAGE = "AI service is temporarily unavailable. Please try again later.";
    private static final String EMPTY_ANSWER_MESSAGE = "AI did not return an answer.";

    private final RestTemplate restTemplate;
    private final String ollamaBaseUrl;
    private final String ollamaModel;

    public OllamaAiService(
            @Qualifier("ollamaRestTemplate") RestTemplate restTemplate,
            @Value("${ai.ollama.base-url}") String ollamaBaseUrl,
            @Value("${ai.ollama.model}") String ollamaModel
    ) {
        this.restTemplate = restTemplate;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaModel = ollamaModel;
    }

    public AiChatResponse chat(String studentMessage) {
        try {
            String answer = generateText(SYSTEM_PROMPT, studentMessage == null ? "" : studentMessage.trim(), 0.3, 120);
            return new AiChatResponse(answer);
        } catch (IllegalStateException exception) {
            return new AiChatResponse(FALLBACK_MESSAGE);
        }
    }

    public String generateText(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        LOGGER.info("AI request received: systemLength={}, userLength={}",
                systemPrompt == null ? 0 : systemPrompt.length(),
                userPrompt == null ? 0 : userPrompt.length());

        try {
            OllamaChatRequest request = new OllamaChatRequest(
                    ollamaModel,
                    List.of(
                            new OllamaChatRequest.Message("system", systemPrompt == null ? "" : systemPrompt.trim()),
                            new OllamaChatRequest.Message("user", userPrompt == null ? "" : userPrompt.trim())
                    ),
                    false,
                    new OllamaChatRequest.Options(temperature, maxTokens)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ngrok-skip-browser-warning", "true");

            HttpEntity<OllamaChatRequest> entity = new HttpEntity<>(request, headers);
            LOGGER.info("Ollama call started: baseUrl={}, model={}", normalizeBaseUrl(ollamaBaseUrl), ollamaModel);
            ResponseEntity<OllamaChatResponse> response = restTemplate.postForEntity(
                    normalizeBaseUrl(ollamaBaseUrl) + "/api/chat",
                    entity,
                    OllamaChatResponse.class
            );

            String answer = extractAnswer(response.getBody());
            LOGGER.info("Ollama call success: answerLength={}", answer.length());
            return answer;
        } catch (RestClientException exception) {
            LOGGER.warn("Ollama call failed with exception", exception);
            throw new IllegalStateException(FALLBACK_MESSAGE, exception);
        } catch (Exception exception) {
            LOGGER.error("Ollama call failed with exception", exception);
            throw new IllegalStateException(FALLBACK_MESSAGE, exception);
        }
    }

    private String extractAnswer(OllamaChatResponse response) {
        if (response == null || response.message() == null || response.message().content() == null) {
            return EMPTY_ANSWER_MESSAGE;
        }

        String content = response.message().content().trim();
        return content.isEmpty() ? EMPTY_ANSWER_MESSAGE : content;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
