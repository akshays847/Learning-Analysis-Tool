package com.lat.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Minimal Claude API client using java.net.http (no SDK needed).
 * Set env var ANTHROPIC_API_KEY to go live. Uses the /v1/messages endpoint.
 */
public class LlmClient {

    private static final ObjectMapper M = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private final String apiKey = System.getenv("ANTHROPIC_API_KEY");
    private final String model = "claude-sonnet-4-6";

    public boolean isLive() { return apiKey != null && !apiKey.isBlank(); }

    /** Send a single-user-message prompt, return the model's text. */
    public String complete(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 2000,
                "messages", new Object[]{ Map.of("role", "user", "content", prompt) }
        );
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(M.writeValueAsString(body)))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = M.readTree(resp.body());
        // response text lives at content[0].text
        return root.path("content").path(0).path("text").asText();
    }

    /** Strip ```json fences an LLM sometimes adds despite instructions. */
    public static String stripFences(String s) {
        return s.strip()
                .replaceAll("(?m)^```(json)?", "")
                .replaceAll("(?m)```$", "")
                .strip();
    }
}
