package com.lat.extract;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Reads a teacher's .docx and returns its text.
 * That text then goes to the LLM for concept extraction.
 * (For a Meet session, the Whisper transcript text takes this file's place.)
 */
public class WordExtractor {

    public static String extractText(String docxPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(docxPath);
             XWPFDocument doc = new XWPFDocument(fis)) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.joining("\n"));
        }
    }
}
