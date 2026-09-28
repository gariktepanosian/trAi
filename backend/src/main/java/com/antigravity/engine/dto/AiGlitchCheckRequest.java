package com.antigravity.engine.dto;

public class AiGlitchCheckRequest {
    private String prompt;
    private String aiResponse;
    private String modelName; // e.g. "GPT-4", "Claude 3.5 Sonnet", "Gemini"

    public AiGlitchCheckRequest() {
    }

    public AiGlitchCheckRequest(String prompt, String aiResponse, String modelName) {
        this.prompt = prompt;
        this.aiResponse = aiResponse;
        this.modelName = modelName;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
