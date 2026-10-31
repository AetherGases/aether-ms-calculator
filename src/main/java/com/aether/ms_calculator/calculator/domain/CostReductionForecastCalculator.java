package com.aether.ms_calculator.calculator.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CostReductionForecastCalculator {

  public CostReductionForecastResult calculate(CostReductionForecastParsed parsed) {
    BigDecimal monthlyCost = normalizeMoney(parsed.monthlyCost());
    BigDecimal monthlySavings =
        monthlyCost
            .multiply(BigDecimal.valueOf(parsed.reductionPercentage()))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

    BigDecimal newMonthlyCost = scaleMoney(monthlyCost.subtract(monthlySavings));
    BigDecimal totalSavings =
        scaleMoney(monthlySavings.multiply(BigDecimal.valueOf(parsed.periodMonths())));

    List<CostReductionForecastResult.CostReductionProjectionPoint> projection =
        new ArrayList<>(parsed.periodMonths());
    for (int month = 1; month <= parsed.periodMonths(); month++) {
      BigDecimal accumulated =
          scaleMoney(monthlySavings.multiply(BigDecimal.valueOf(month)));
      projection.add(
          new CostReductionForecastResult.CostReductionProjectionPoint(month, accumulated));
    }

    return new CostReductionForecastResult(
        monthlyCost,
        parsed.reductionPercentage(),
        parsed.periodMonths(),
        monthlySavings,
        newMonthlyCost,
        totalSavings,
        projection);
  }

  private static BigDecimal normalizeMoney(BigDecimal value) {
    BigDecimal normalized = value.stripTrailingZeros();
    if (normalized.scale() < 0) {
      return normalized.setScale(0);
    }
    return normalized;
  }

  private static BigDecimal scaleMoney(BigDecimal value) {
    return value.setScale(2, RoundingMode.UNNECESSARY);
  }
}
