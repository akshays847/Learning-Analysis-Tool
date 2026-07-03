package com.lat.extract;

import com.lat.model.StudentBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Google Meet chat export (saved to Drive when the meeting is recorded).
 * Each line looks like:  2026-06-28 10:14:03  Priya: is chlorophyll ...
 * Messages are grouped per student into bundles for assessment.
 */
public class ChatParser {

    // timestamp (loose) + Name + ": " + message
    private static final Pattern LINE =
            Pattern.compile("^([\\d\\-: ]+?)\\s+([^:]+?):\\s+(.+)$");

    /** Parse from a file path. */
    public static List<StudentBundle> parse(String chatPath) throws IOException {
        return parseText(Files.readString(Path.of(chatPath)));
    }

    /** Parse from raw chat text (used by the web upload + tests). */
    public static List<StudentBundle> parseText(String chatText) {
        // LinkedHashMap keeps students in first-seen order (stable output)
        Map<String, List<String>> byStudent = new LinkedHashMap<>();
        for (String raw : chatText.split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty()) continue;
            Matcher m = LINE.matcher(line);
            if (m.matches()) {
                String name = m.group(2).strip();
                String msg  = m.group(3).strip();
                byStudent.computeIfAbsent(name, k -> new ArrayList<>()).add(msg);
            }
        }
        List<StudentBundle> bundles = new ArrayList<>();
        byStudent.forEach((name, msgs) -> bundles.add(new StudentBundle(name, msgs)));
        return bundles;
    }
}
