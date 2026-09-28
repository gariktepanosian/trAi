package com.trai.engine.controller;

import com.trai.engine.translation.TranslationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/translate")
@CrossOrigin(origins = "*")
public class TranslationController {

    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> translate(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");
        String targetLanguage = body.getOrDefault("targetLanguage", "en");
        Map<String, Object> result = translationService.translateText(text, targetLanguage);
        return ResponseEntity.ok(result);
    }
}
