package com.trai.engine.translation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TranslationService Unit Tests")
class TranslationServiceTest {

    private final TranslationService service = new TranslationService();

    @Test
    @DisplayName("Should translate to Russian prefix")
    void testTranslateToRussian() {
        Map<String, Object> result = service.translateText("Central bank cuts rates", "ru");
        assertNotNull(result);
        assertTrue(((String) result.get("translated")).contains("[RU Перевод]"));
        assertEquals("ru", result.get("targetLanguage"));
    }

    @Test
    @DisplayName("Should translate to Armenian prefix")
    void testTranslateToArmenian() {
        Map<String, Object> result = service.translateText("Breaking truth alert", "hy");
        assertNotNull(result);
        assertTrue(((String) result.get("translated")).contains("[HY Թարգմանություն]"));
        assertEquals("hy", result.get("targetLanguage"));
    }

    @Test
    @DisplayName("Should handle empty text gracefully")
    void testEmptyText() {
        Map<String, Object> result = service.translateText("", "ru");
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
    }
}
