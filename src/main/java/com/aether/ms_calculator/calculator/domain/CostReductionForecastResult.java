package com.aether.ms_calculator.calculator.domain;

import java.math.BigDecimal;
import java.util.List;

public record CostReductionForecastResult(
    BigDecimal monthlyCost,
    int reductionPercentage,
    int periodMonths,
    BigDecimal monthlySavings,
    BigDecimal newMonthlyCost,
    BigDecimal totalSavings,
    List<CostReductionProjectionPoint> projection) {

  public record CostReductionProjectionPoint(int month, BigDecimal accumulatedSavings) {}
}
