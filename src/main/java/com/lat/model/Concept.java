package com.lat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** One concept taught in the session — the ground truth students are judged against. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Concept(
        String id,             // "c01"
        String concept,        // topic name
        String type,           // "definition" | "process" | "application"
        List<String> keyPoints,// atomic facts the teacher made
        String importance      // "core" | "supplementary"
) {}
