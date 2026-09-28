package com.trai.engine.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import com.trai.engine.ai.AntiPropagandaEngine;
import com.trai.engine.ai.LiveFactCheckEngine;
import com.trai.engine.ai.AiGlitchVerifierEngine;
import com.trai.engine.analytics.MarketImpactEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${xai.api-key:your-xai-api-key}")
    private String xaiApiKey;

    @Bean
    public ChatLanguageModel grokModel() {
        String key = (xaiApiKey != null && !xaiApiKey.isBlank()) ? xaiApiKey : "demo-key";
        return OpenAiChatModel.builder()
                .apiKey(key)
                .baseUrl("https://api.x.ai/v1")
                .modelName("grok-2-latest")
                .build();
    }

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

