package com.lat;

import com.lat.compare.Summarizer;
import com.lat.extract.ChatParser;
import com.lat.model.*;
import com.lat.service.AnalysisService;
import com.lat.service.AnalysisService.AnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test evidence for the Learning Analysis Tool.
 * Covers: chat parsing, scoring math, edge cases, and end-to-end pipeline
 * against known-answer (golden) data.
 */
class AnalysisTest {

    // ---------- CHAT PARSING ----------
    @Nested
    @DisplayName("Meet chat parsing")
    class ChatParsing {

        @Test
        @DisplayName("groups messages by student in first-seen order")
        void groupsByStudent() {
            String chat = """
                2026-06-28 10:14:03  Priya: hi
                2026-06-28 10:14:40  Rahul: hello
                2026-06-28 10:15:20  Priya: second message
                """;
            List<StudentBundle> b = ChatParser.parseText(chat);
            assertEquals(2, b.size());
            assertEquals("Priya", b.get(0).student());
            assertEquals(2, b.get(0).messages().size());
            assertEquals("second message", b.get(0).messages().get(1));
        }

        @Test
        @DisplayName("ignores blank and malformed lines")
        void ignoresJunk() {
            String chat = """
                2026-06-28 10:14:03  Priya: valid

                --- meeting started ---
                2026-06-28 10:15:00  Rahul: also valid
                """;
            List<StudentBundle> b = ChatParser.parseText(chat);
            assertEquals(2, b.size());  // the two valid lines only
        }

        @Test
        @DisplayName("handles empty chat without crashing")
        void emptyChat() {
            assertTrue(ChatParser.parseText("").isEmpty());
        }

        @Test
        @DisplayName("preserves colons inside a message")
        void colonInMessage() {
            String chat = "2026-06-28 10:14:03  Priya: ratio is 3:1 here";
            List<StudentBundle> b = ChatParser.parseText(chat);
            assertEquals("ratio is 3:1 here", b.get(0).messages().get(0));
        }
    }

    // ---------- SCORING MATH ----------
    @Nested
    @DisplayName("Coverage scoring")
    class Scoring {

        private final List<Concept> concepts = List.of(
                new Concept("c01", "A", "definition", List.of(), "core"),
                new Concept("c02", "B", "process", List.of(), "core"),
                new Concept("c03", "C", "process", List.of(), "core"),
                new Concept("c04", "D", "application", List.of(), "supplementary"));

        @Test
        @DisplayName("all mastery = 100%")
        void fullMastery() {
            var a = List.of(
                    new Assessment("c01", "mastery", "", ""),
                    new Assessment("c02", "mastery", "", ""),
                    new Assessment("c03", "mastery", "", ""),
                    new Assessment("c04", "mastery", "", ""));
            StudentReport r = Summarizer.summarizeStudent(concepts, "X", a, List.of());
            assertEquals(100.0, r.coveragePct());
            assertEquals(4, r.strengths().size());
            assertTrue(r.gaps().isEmpty());
        }

        @Test
        @DisplayName("all absent = 0%")
        void allAbsent() {
            var a = List.of(
                    new Assessment("c01", "absent", "", ""),
                    new Assessment("c02", "absent", "", ""),
                    new Assessment("c03", "absent", "", ""),
                    new Assessment("c04", "absent", "", ""));
            StudentReport r = Summarizer.summarizeStudent(concepts, "X", a, List.of());
            assertEquals(0.0, r.coveragePct());
            assertEquals(4, r.gaps().size());
        }

        @Test
        @DisplayName("mixed levels compute weighted coverage")
        void mixedLevels() {
            // mastery(3)+understood(2)+partial(1)+absent(0) = 6 of 12 = 50%
            var a = List.of(
                    new Assessment("c01", "mastery", "", ""),
                    new Assessment("c02", "understood", "", ""),
                    new Assessment("c03", "partial", "", ""),
                    new Assessment("c04", "absent", "", ""));
            StudentReport r = Summarizer.summarizeStudent(concepts, "X", a, List.of());
            assertEquals(50.0, r.coveragePct());
            // partial + absent both count as gaps
            assertEquals(2, r.gaps().size());
            assertEquals(2, r.strengths().size());
        }

        @Test
        @DisplayName("partial counts as a gap, not a strength")
        void partialIsGap() {
            var a = List.of(new Assessment("c01", "partial", "", ""));
            var single = List.of(concepts.get(0));
            StudentReport r = Summarizer.summarizeStudent(single, "X", a, List.of());
            assertTrue(r.gaps().contains("A"));
            assertTrue(r.strengths().isEmpty());
        }
    }

    // ---------- CLASS WEAK SPOTS ----------
    @Nested
    @DisplayName("Class weak spots")
    class WeakSpots {

        @Test
        @DisplayName("ranks concepts by how many students missed them")
        void ranksByMissCount() {
            var r1 = new StudentReport("A", 0, List.of("X", "Y"), List.of(), List.of());
            var r2 = new StudentReport("B", 0, List.of("X"), List.of(), List.of());
            var weak = Summarizer.classWeakSpots(List.of(r1, r2));
            assertEquals("X", weak.get(0).getKey());   // missed by 2, ranks first
            assertEquals(2, weak.get(0).getValue());
            assertEquals("Y", weak.get(1).getKey());
        }
    }

    // ---------- END TO END (golden data via mock) ----------
    @Nested
    @DisplayName("End-to-end pipeline")
    class EndToEnd {

        @Test
        @DisplayName("produces expected reports on golden sample")
        void goldenSample() throws Exception {
            String chat = """
                2026-06-28 10:14:03  Priya: is chlorophyll the same as chloroplast?
                2026-06-28 10:16:01  Priya: Calvin cycle uses ATP to fix carbon
                2026-06-28 10:14:40  Rahul: light reaction makes ATP
                2026-06-28 10:19:12  Amit: photosynthesis just makes food
                """;
            AnalysisResult res = new AnalysisService().analyze("lesson text", chat);

            assertEquals(3, res.reports().size());
            // Priya (mock) should outscore Amit (mock)
            double priya = res.reports().stream()
                    .filter(r -> r.student().equals("Priya")).findFirst().get().coveragePct();
            double amit = res.reports().stream()
                    .filter(r -> r.student().equals("Amit")).findFirst().get().coveragePct();
            assertTrue(priya > amit, "Priya should score higher than Amit");
            // rate factors should be a class-wide weak spot
            assertFalse(res.classWeakSpots().isEmpty());
        }
    }
}
