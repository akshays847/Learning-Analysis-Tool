package com.lat.model;

import java.util.List;

/** Final per-student summary shown on the dashboard. */
public record StudentReport(
        String student,
        double coveragePct,
        List<String> gaps,
        List<String> strengths,
        List<String> curiosity
) {}
