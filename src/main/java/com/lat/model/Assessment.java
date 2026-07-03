package com.lat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** LLM's judgement of one student on one concept. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Assessment(
        String conceptId,
        String level,     // "absent" | "partial" | "understood" | "mastery"
        String evidence,
        String gap
) {}
