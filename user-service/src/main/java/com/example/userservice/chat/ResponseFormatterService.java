package com.example.userservice.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResponseFormatterService {

    private final RestTemplate restTemplate;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${openai.model:gpt-4.1-mini}")
    private String openAiModel;

    public ResponseFormatterService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Format arbitrary text into safe HTML using an AI formatter; falls back to a simple HTML
     * conversion if the AI call fails.
     */
    public String formatHtml(String raw) {
        String normalized = normalize(raw);
        if (normalized == null || normalized.isBlank()) {
            return normalized;
        }
        if (isAlreadyHtml(normalized)) {
            return normalized;
        }
        try {
            return aiFormatToHtml(normalized);
        } catch (Exception ex) {
            // Fallback to deterministic local formatter
            return localHtml(normalized);
        }
    }

    private String aiFormatToHtml(String text) throws Exception {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key missing for formatter");
        }
        String url = openAiBaseUrl + "/chat/completions";

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", """
                        You are an HTML formatter. Convert the given plain text/markdown into clean, safe HTML.
                        - Preserve headings, lists, and paragraphs.
                        - Use <p>, <ul>/<ol>, <li>, <strong>/<em> where appropriate.
                        - Do not invent content. Do not wrap in html/body.
                        - Return only the HTML fragment.
                        """
        );
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", text
        );

        Map<String, Object> body = new HashMap<>();
        body.put("model", openAiModel);
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("temperature", 0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String raw = restTemplate.postForObject(url, entity, String.class);
        return extractContent(raw);
    }

    private String extractContent(String rawJson) throws Exception {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalStateException("Empty formatter response");
        }
        com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(rawJson);
        com.fasterxml.jackson.databind.JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("No choices in formatter response");
        }
        com.fasterxml.jackson.databind.JsonNode message = choices.get(0).path("message");
        String content = message.path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Formatter content empty");
        }
        return content;
    }

    private String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.replace("\r\n", "\n");
        normalized = normalizeBullets(normalized);
        return normalized;
    }

    private boolean isAlreadyHtml(String text) {
        return text.contains("<p>") || text.contains("<ul>") || text.contains("<li>") || text.contains("<ol>");
    }

    private String localHtml(String normalized) {
        StringBuilder html = new StringBuilder();
        boolean inUnordered = false;
        boolean inOrdered = false;

        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (inUnordered) {
                    html.append("</ul>");
                    inUnordered = false;
                }
                if (inOrdered) {
                    html.append("</ol>");
                    inOrdered = false;
                }
                continue;
            }

            if (trimmed.startsWith("- ")) {
                if (!inUnordered) {
                    if (inOrdered) {
                        html.append("</ol>");
                        inOrdered = false;
                    }
                    html.append("<ul>");
                    inUnordered = true;
                }
                html.append("<li>").append(escape(trimmed.substring(2).trim())).append("</li>");
            } else if (trimmed.matches("^\\d+\\.\\s+.*")) {
                if (!inOrdered) {
                    if (inUnordered) {
                        html.append("</ul>");
                        inUnordered = false;
                    }
                    html.append("<ol>");
                    inOrdered = true;
                }
                html.append("<li>").append(escape(trimmed.replaceFirst("^\\d+\\.\\s+", ""))).append("</li>");
            } else {
                if (inUnordered) {
                    html.append("</ul>");
                    inUnordered = false;
                }
                if (inOrdered) {
                    html.append("</ol>");
                    inOrdered = false;
                }
                html.append("<p>").append(escape(trimmed)).append("</p>");
            }
        }

        if (inUnordered) {
            html.append("</ul>");
        }
        if (inOrdered) {
            html.append("</ol>");
        }

        return html.toString();
    }

    private String normalizeBullets(String text) {
        return text
                .replace("•", "- ")
                .replace("\u2022", "- ");
    }

    private String escape(String text) {
        return HtmlUtils.htmlEscape(text);
    }
}

