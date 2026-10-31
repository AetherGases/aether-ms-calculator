package com.aether.ms_calculator.calculator.domain;

import java.math.BigDecimal;

public record CostReductionForecastParsed(
    BigDecimal monthlyCost, int reductionPercentage, int periodMonths) {}
