package com.trai.engine.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import com.trai.engine.ai.AntiPropagandaEngine;
import com.trai.engine.ai.LiveFactCheckEngine;
import com.trai.engine.ai.AiGlitchVerifierEngine;
import com.trai.engine.analytics.MarketImpactEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI Model Configuration — Three active models:
 *
 *  1. Grok-4.7      (xAI)          — X-native, real user context, Grok scoring (PRIMARY)
 *  2. Gemini 2.5 Flash (Google)    — Fast multimodal, Google knowledge graph (SECONDARY)
 *  3. GPT-4o        (OpenAI)       — ChatGPT filter + cross-validation (TERTIARY)
 *
 *  All three are used in the multi-agent parallel validation pipeline
 *  (see MultiAgentValidationService).
 *
 *  Model versions are pinned to specific stable releases.
 *  Never use rolling "latest" aliases in production.
 */
@Configuration
public class AiConfig {

    // ─── xAI Grok ────────────────────────────────────────────────────────────
    @Value("${xai.api-key:demo-key}")
    private String xaiApiKey;

    @Value("${xai.model:grok-4.7}")
    private String xaiModel;

    // ─── Google Gemini ────────────────────────────────────────────────────────
    @Value("${langchain4j.vertex-ai.gemini.project:your-project-id}")
    private String geminiProjectId;

    @Value("${langchain4j.vertex-ai.gemini.location:us-central1}")
    private String geminiLocation;

    @Value("${langchain4j.vertex-ai.gemini.model-name:gemini-2.5-flash}")
    private String geminiModel;

    // ─── OpenAI ChatGPT ───────────────────────────────────────────────────────
    @Value("${openai.api-key:demo-key}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4o}")
    private String openAiModel;

    // ─────────────────────────────────────────────────────────────────────────
    // Grok-4.7 model bean (PRIMARY — pinned stable version, not rolling alias)
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    @Primary
    public ChatLanguageModel grokModel() {
        String key = (xaiApiKey != null && !xaiApiKey.isBlank() && !xaiApiKey.equals("demo-key"))
                ? xaiApiKey : "demo-key";
        return OpenAiChatModel.builder()
                .apiKey(key)
                .baseUrl("https://api.x.ai/v1")
                .modelName(xaiModel)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gemini 2.5 Flash model bean (SECONDARY — fast Google multimodal)
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    public ChatLanguageModel geminiModel() {
        if (geminiProjectId == null || geminiProjectId.isBlank()
                || geminiProjectId.equals("your-project-id")) {
            // Fallback: use Grok when Gemini credentials are not configured
            return grokModel();
        }
        return GoogleAiGeminiChatModel.builder()
                .project(geminiProjectId)
                .location(geminiLocation)
                .modelName(geminiModel)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ChatGPT-4o model bean (TERTIARY — OpenAI cross-validation filter)
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    public ChatLanguageModel chatGptModel() {
        String key = (openAiApiKey != null && !openAiApiKey.isBlank()
                && !openAiApiKey.equals("demo-key") && !openAiApiKey.startsWith("sk-REPLACE"))
                ? openAiApiKey : "demo-key";
        return OpenAiChatModel.builder()
                .apiKey(key)
                .modelName(openAiModel)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI Engine Interfaces — all wired to Grok as the PRIMARY engine.
    // The multi-agent pipeline (MultiAgentValidationService) calls all three
    // models in parallel independently — these beans are for single-model APIs.
    // Also used by XUserRankingService and SourceAuthorityService.
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public AntiPropagandaEngine antiPropagandaEngine(ChatLanguageModel grokModel) {
        return AiServices.builder(AntiPropagandaEngine.class)
                .chatLanguageModel(grokModel)
                .build();
    }

    @Bean
    public LiveFactCheckEngine liveFactCheckEngine(ChatLanguageModel grokModel) {
        return AiServices.builder(LiveFactCheckEngine.class)
                .chatLanguageModel(grokModel)
                .build();
    }

    @Bean
    public AiGlitchVerifierEngine aiGlitchVerifierEngine(ChatLanguageModel grokModel) {
        return AiServices.builder(AiGlitchVerifierEngine.class)
                .chatLanguageModel(grokModel)
                .build();
    }

    @Bean
    public MarketImpactEngine marketImpactEngine(ChatLanguageModel grokModel) {
        return AiServices.builder(MarketImpactEngine.class)
                .chatLanguageModel(grokModel)
                .build();
    }
}
