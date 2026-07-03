package com.lat.compare;

import com.lat.model.*;

import java.util.*;

/**
 * Turns per-concept assessments into coverage %, gaps, strengths,
 * and class-wide weak spots (concepts most students missed = teaching signal).
 */
public class Summarizer {

    private static final Map<String, Integer> SCORE = Map.of(
            "absent", 0, "partial", 1, "understood", 2, "mastery", 3);

    /** Build one student's report. */
    public static StudentReport summarizeStudent(
            List<Concept> concepts, String student,
            List<Assessment> assessments, List<String> curiosity) {

        Map<String, String> idToName = new HashMap<>();
        for (Concept c : concepts) idToName.put(c.id(), c.concept());

        int n = Math.max(concepts.size(), 1);
        int got = 0;
        List<String> gaps = new ArrayList<>();
        List<String> strengths = new ArrayList<>();

        for (Assessment a : assessments) {
            got += SCORE.getOrDefault(a.level(), 0);
            String name = idToName.getOrDefault(a.conceptId(), a.conceptId());
            switch (a.level()) {
                case "absent", "partial" -> gaps.add(name);
                case "understood", "mastery" -> strengths.add(name);
            }
        }
        double coverage = Math.round(1000.0 * got / (n * 3)) / 10.0;
        return new StudentReport(student, coverage, gaps, strengths,
                curiosity == null ? List.of() : curiosity);
    }

    /** Count how many students missed each concept -> class weak spots, most-missed first. */
    public static List<Map.Entry<String, Integer>> classWeakSpots(List<StudentReport> reports) {
        Map<String, Integer> count = new HashMap<>();
        for (StudentReport r : reports)
            for (String gap : r.gaps())
                count.merge(gap, 1, Integer::sum);
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(count.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        return sorted;
    }
}
