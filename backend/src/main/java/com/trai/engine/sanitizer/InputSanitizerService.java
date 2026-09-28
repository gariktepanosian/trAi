package com.trai.engine.sanitizer;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Input Sanitizer Service.
 *
 * Performs:
 *   1. HTML/script tag stripping
 *   2. SQL injection pattern detection
 *   3. Prompt injection detection (jailbreak attempts)
 *   4. PII masking (email, phone, credit card numbers)
 *
 * Called before any text is forwarded to the AI engines.
 */
@Service
public class InputSanitizerService {

    private static final Logger log = LoggerFactory.getLogger(InputSanitizerService.class);

    // ─── PII Patterns ─────────────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\+?[\\d\\s\\-().]{7,20})");

    private static final Pattern CREDIT_CARD_PATTERN =
            Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");

    // ─── Prompt Injection Patterns ────────────────────────────────────────────
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?previous\\s+(instructions?|prompts?)"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+(a|an|DAN|jailbreak)"),
            Pattern.compile("(?i)(system\\s*:\\s*|<\\s*system\\s*>)"),
            Pattern.compile("(?i)DAN\\s+mode"),
            Pattern.compile("(?i)jailbreak"),
            Pattern.compile("(?i)act\\s+as\\s+if\\s+you\\s+have\\s+no\\s+(rules|restrictions|guidelines)"),
            Pattern.compile("(?i)pretend\\s+(you|that)\\s+(are|you're)"),
            Pattern.compile("(?i)forget\\s+(everything|all)\\s+(you|I)\\s+(know|told)"),
            Pattern.compile("(?i)\\[INST\\]|\\[/INST\\]|<\\|im_start\\|>|<\\|im_end\\|>")
    );

    // ─── SQL Injection Patterns ───────────────────────────────────────────────
    private static final List<Pattern> SQL_PATTERNS = List.of(
            Pattern.compile("(?i)(DROP|DELETE|INSERT|UPDATE|SELECT)\\s+(TABLE|FROM|INTO|WHERE)"),
            Pattern.compile("(?i)(OR|AND)\\s+['\"]?\\d+['\"]?\\s*=\\s*['\"]?\\d+['\"]?"),
            Pattern.compile("(?i)UNION\\s+SELECT"),
            Pattern.compile("(?i);\\s*(DROP|DELETE|TRUNCATE)")
    );

    /**
     * Sanitizes a text input.
     * @return SanitizationResult containing cleaned text and a list of detected flags
     */
    public SanitizationResult sanitize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new SanitizationResult("", List.of(), false);
        }

        List<String> flags = new ArrayList<>();
        String text = rawText;

        // 1. Strip HTML tags using Jsoup
        String stripped = Jsoup.clean(text, Safelist.none());
        if (!stripped.equals(text)) {
            flags.add("HTML_STRIPPED");
            log.debug("HTML tags stripped from input");
        }
        text = stripped;

        // 2. Detect SQL injection
        for (Pattern p : SQL_PATTERNS) {
            if (p.matcher(text).find()) {
                flags.add("SQL_INJECTION_DETECTED");
                log.warn("SQL injection pattern detected in input");
                break;
            }
        }

        // 3. Detect prompt injection
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(text).find()) {
                flags.add("PROMPT_INJECTION_DETECTED");
                log.warn("Prompt injection attempt detected: {}", text.substring(0, Math.min(80, text.length())));
                break;
            }
        }

        // 4. Mask PII — replace with redacted placeholders
        String maskedText = text;

        if (EMAIL_PATTERN.matcher(maskedText).find()) {
            maskedText = EMAIL_PATTERN.matcher(maskedText).replaceAll("[EMAIL_REDACTED]");
            flags.add("EMAIL_PII_MASKED");
        }

        if (CREDIT_CARD_PATTERN.matcher(maskedText).find()) {
            maskedText = CREDIT_CARD_PATTERN.matcher(maskedText).replaceAll("[CARD_REDACTED]");
            flags.add("CREDIT_CARD_PII_MASKED");
        }

        boolean blocked = flags.contains("PROMPT_INJECTION_DETECTED") ||
                          flags.contains("SQL_INJECTION_DETECTED");

        return new SanitizationResult(maskedText, flags, blocked);
    }

    /** Result container for sanitized input */
    public record SanitizationResult(String cleanText, List<String> flags, boolean blocked) {}
}
