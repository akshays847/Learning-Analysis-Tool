package com.lat.util;

/** The two LLM prompts that do the real analytical work. */
public final class Prompts {

    public static String concepts(String transcript) {
        return """
            You are analyzing a teacher's lesson (transcript or notes).
            Extract every distinct concept TAUGHT. For each return:
            - id: short stable id like "c01"
            - concept: the topic name
            - type: one of "definition" | "process" | "application"
            - keyPoints: list of atomic facts/ideas (one idea per string)
            - importance: "core" or "supplementary"

            Return ONLY a JSON array. No prose, no markdown fences.

            LESSON:
            """ + transcript;
    }

    public static String assess(String conceptsJson, String student, String messagesJson) {
        return """
            You judge how well a student understood a lesson,
            using ONLY their chat messages as evidence.

            For EACH concept output an object:
            - conceptId
            - level: "absent" | "partial" | "understood" | "mastery"
            - evidence: paraphrase from their chat justifying the level (or "no evidence")
            - gap: what's missing or wrong, if anything

            Return ONLY JSON: {"assessments":[...], "curiosity":[...]}.
            "curiosity" = things the student explored BEYOND the taught concepts.
            No markdown fences.

            CONCEPTS:
            """ + conceptsJson + """

            STUDENT (""" + student + ") CHAT:\n" + messagesJson;
    }

    private Prompts() {}
}
