package com.aether.ms_calculator.calculator.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

public record CostReductionForecastParsed(
    JsonNode monthlyCostEcho,
    JsonNode reductionPercentageEcho,
    JsonNode periodMonthsEcho,
    BigDecimal monthlyCost,
    int reductionPercentage,
    int periodMonths) {}
