package com.aether.ms_calculator.calculator.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record RoiResult(
        JsonNode capexEcho,
        JsonNode waccMonthlyEcho,
        int months,
        double vpTotal,
        double vpl,
        double roiPercent,
        boolean viable) {}
