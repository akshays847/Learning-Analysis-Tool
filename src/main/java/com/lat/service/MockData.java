package com.lat.service;

import com.lat.model.Assessment;
import com.lat.model.Concept;
import com.lat.service.AnalysisService.ParsedAssess;

import java.util.*;

/** Deterministic sample data so the app + tests run without an API key. */
public final class MockData {

    public static List<Concept> concepts() {
        return List.of(
            new Concept("c01","Chloroplast vs Chlorophyll","definition",
                List.of("chloroplast is the organelle","chlorophyll is the pigment inside"),"core"),
            new Concept("c02","Light Reaction","process",
                List.of("needs light","produces ATP"),"core"),
            new Concept("c03","Calvin Cycle (Dark Reaction)","process",
                List.of("does not need light directly","uses ATP","fixes carbon into glucose"),"core"),
            new Concept("c04","Factors affecting rate","application",
                List.of("temperature","light intensity","CO2 concentration"),"supplementary")
        );
    }

    public static Map<String, ParsedAssess> assessments() {
        Map<String, ParsedAssess> m = new LinkedHashMap<>();
        m.put("Priya", new ParsedAssess(List.of(
            new Assessment("c01","mastery","chloroplast=organelle, chlorophyll=pigment",""),
            new Assessment("c02","understood","implied via ATP in Calvin cycle",""),
            new Assessment("c03","mastery","Calvin cycle uses ATP to fix carbon into glucose",""),
            new Assessment("c04","partial","asked if temperature affects rate","only raised question")
        ), List.of("enzyme/temperature link to reaction rate")));
        m.put("Rahul", new ParsedAssess(List.of(
            new Assessment("c01","absent","no evidence","never discussed"),
            new Assessment("c02","understood","light reaction makes ATP",""),
            new Assessment("c03","partial","dark reaction uses ATP, no light","missed carbon fixation"),
            new Assessment("c04","absent","no evidence","never discussed")
        ), List.of()));
        m.put("Amit", new ParsedAssess(List.of(
            new Assessment("c01","absent","no evidence","never discussed"),
            new Assessment("c02","absent","no evidence","confused light vs dark"),
            new Assessment("c03","partial","asked difference light/dark","still confused"),
            new Assessment("c04","absent","no evidence","never discussed")
        ), List.of()));
        return m;
    }

    private MockData() {}
}
