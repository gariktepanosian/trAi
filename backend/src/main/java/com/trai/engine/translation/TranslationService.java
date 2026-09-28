package com.trai.engine.translation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Multi-Language Translation Service.
 * Provides localized truth intelligence in English (EN), Russian (RU), and Armenian (HY).
 */
@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

    @Cacheable(value = "translations", key = "#targetLang + ':' + #text.hashCode()")
    public Map<String, Object> translateText(String text, String targetLang) {
        if (text == null || text.isBlank()) {
            return Map.of("error", "Text is required for translation.");
        }

        String target = (targetLang != null) ? targetLang.toLowerCase().trim() : "en";
        log.info("Translating payload to [{}]: {}", target, text.substring(0, Math.min(40, text.length())));

        String translated = mockTranslate(text, target);

        return Map.of(
                "original", text,
                "translated", translated,
                "targetLanguage", target,
                "timestamp", Instant.now().toString()
        );
    }

    private String mockTranslate(String text, String target) {
        return switch (target) {
            case "ru" -> "[RU Перевод] " + text;
            case "hy", "am" -> "[HY Թարգմանություն] " + text;
            default -> text;
        };
    }
}
