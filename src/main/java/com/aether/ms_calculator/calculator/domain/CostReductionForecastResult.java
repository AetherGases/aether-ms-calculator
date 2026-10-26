package com.aether.ms_calculator.calculator.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;

public record CostReductionForecastResult(
    JsonNode monthlyCostEcho,
    JsonNode reductionPercentageEcho,
    JsonNode periodMonthsEcho,
    BigDecimal monthlySavings,
    BigDecimal newMonthlyCost,
    BigDecimal totalSavings,
    List<CostReductionProjectionPoint> projection) {

  public record CostReductionProjectionPoint(int month, BigDecimal accumulatedSavings) {}
}
