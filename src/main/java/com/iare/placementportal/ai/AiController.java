package com.iare.placementportal.ai;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final OllamaAiService ollamaAiService;

    public AiController(OllamaAiService ollamaAiService) {
        this.ollamaAiService = ollamaAiService;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return ollamaAiService.chat(request.message());
    }
}
