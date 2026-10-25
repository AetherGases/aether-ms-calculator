package com.aether.ms_calculator.calculator.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record PaybackResult(
        JsonNode capexEcho,
        JsonNode waccMonthlyEcho,
        int months,
        double vpTotal,
        int paybackMonths,
        boolean viable) {}
