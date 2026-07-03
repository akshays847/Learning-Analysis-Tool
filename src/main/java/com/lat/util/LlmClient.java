package com.lat.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Calls Google Gemini's free API (no cost, no card for the basic tier).
 * Set env var GEMINI_API_KEY to go live. Get a key at:
 *   https://aistudio.google.com/apikey
 *
 * If the key is missing, isLive() returns false and the app runs in
 * deterministic mock mode so it still works with zero setup.
 */
public class LlmClient {

    private static final ObjectMapper M = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private final String apiKey = System.getenv("GEMINI_API_KEY");

    // Free-tier model with generous limits (~15 RPM, ~1,500/day as of 2026).
    private final String model = "gemini-2.5-flash";

    public boolean isLive() { return apiKey != null && !apiKey.isBlank(); }

    /**
     * Send a single prompt, return the model's text answer.
     * Retries automatically on 429 (free-tier rate limit) with increasing
     * waits, so multi-student analyses don't crash when calls come too fast.
     */
    public String complete(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{ Map.of("text", prompt) })
                },
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 2048
                )
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(M.writeValueAsString(body)))
                .build();

        int maxAttempts = 4;
        long waitMs = 5000;   // first wait; doubles each retry (5s, 10s, 20s)
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                JsonNode root = M.readTree(resp.body());
                JsonNode textNode = root.path("candidates").path(0)
                        .path("content").path("parts").path(0).path("text");
                if (textNode.isMissingNode()) {
                    throw new RuntimeException("Unexpected Gemini response: " + resp.body());
                }
                return textNode.asText();
            }

            // 429 = rate limited. Wait and retry rather than crashing.
            if (resp.statusCode() == 429 && attempt < maxAttempts) {
                System.out.println("Gemini rate limit hit (429) — waiting "
                        + (waitMs / 1000) + "s then retrying (attempt "
                        + attempt + "/" + maxAttempts + ")");
                Thread.sleep(waitMs);
                waitMs *= 2;
                continue;
            }

            // any other error, or out of retries
            throw new RuntimeException("Gemini API error " + resp.statusCode()
                    + " after " + attempt + " attempt(s): " + resp.body());
        }
        throw new RuntimeException("Gemini API: exhausted retries (rate limited)");
    }

    /** Strip ```json fences the model sometimes adds despite instructions. */
    public static String stripFences(String s) {
        return s.strip()
                .replaceAll("(?m)^```(json)?", "")
                .replaceAll("(?m)```$", "")
                .strip();
    }
}
