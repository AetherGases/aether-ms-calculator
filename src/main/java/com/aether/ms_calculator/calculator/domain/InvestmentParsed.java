package com.aether.ms_calculator.calculator.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record InvestmentParsed(
        JsonNode capexEcho,
        JsonNode waccMonthlyEcho,
        double capex,
        double waccMonthly,
        double[] cashFlows) {}
