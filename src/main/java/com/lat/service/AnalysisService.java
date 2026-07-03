package com.lat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lat.compare.Summarizer;
import com.lat.extract.ChatParser;
import com.lat.model.*;
import com.lat.util.LlmClient;
import com.lat.util.Prompts;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Orchestrates the full analysis pipeline.
 * Used by both the web controller and the JUnit tests.
 */
@Service
public class AnalysisService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final LlmClient llm = new LlmClient();

    /** Result bundle returned to the dashboard. */
    public record AnalysisResult(
            List<Concept> concepts,
            List<StudentReport> reports,
            List<Map.Entry<String, Integer>> classWeakSpots
    ) {}

    /**
     * Run the pipeline.
     * @param lessonText teacher content (from .docx or transcript)
     * @param chatText   raw Meet chat export
     */
    public AnalysisResult analyze(String lessonText, String chatText) throws Exception {
        List<StudentBundle> bundles = ChatParser.parseText(chatText);

        List<Concept> concepts;
        Map<String, ParsedAssess> perStudent = new LinkedHashMap<>();

        if (llm.isLive()) {
            String cJson = LlmClient.stripFences(llm.complete(Prompts.concepts(lessonText)));
            concepts = Arrays.asList(mapper.readValue(cJson, Concept[].class));
            for (StudentBundle b : bundles) {
                String msgs = mapper.writeValueAsString(b.messages());
                String raw = LlmClient.stripFences(
                        llm.complete(Prompts.assess(cJson, b.student(), msgs)));
                perStudent.put(b.student(), parseAssess(raw));
            }
        } else {
            // deterministic mock so the app + tests run without a key
            concepts = MockData.concepts();
            perStudent = MockData.assessments();
        }

        List<StudentReport> reports = new ArrayList<>();
        for (StudentBundle b : bundles) {
            ParsedAssess pa = perStudent.get(b.student());
            if (pa == null) continue;
            reports.add(Summarizer.summarizeStudent(
                    concepts, b.student(), pa.assessments(), pa.curiosity()));
        }
        return new AnalysisResult(concepts, reports, Summarizer.classWeakSpots(reports));
    }

    public record ParsedAssess(List<Assessment> assessments, List<String> curiosity) {}

    private ParsedAssess parseAssess(String json) throws Exception {
        var node = mapper.readTree(json);
        List<Assessment> as = Arrays.asList(
                mapper.treeToValue(node.get("assessments"), Assessment[].class));
        List<String> cur = new ArrayList<>();
        if (node.has("curiosity")) node.get("curiosity").forEach(n -> cur.add(n.asText()));
        return new ParsedAssess(as, cur);
    }
}
