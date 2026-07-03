package com.lat.model;

import java.util.List;

/** A student's chat messages grouped together, ready for assessment. */
public record StudentBundle(String student, List<String> messages) {}
