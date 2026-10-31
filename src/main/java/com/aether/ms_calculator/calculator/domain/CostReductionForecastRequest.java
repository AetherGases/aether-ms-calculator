package com.aether.ms_calculator.calculator.domain;

import java.math.BigDecimal;
import java.util.Set;

public record CostReductionForecastRequest(
    BigDecimal monthlyCost,
    Integer reductionPercentage,
    Integer periodMonths,
    Set<String> unknownFields) {}
