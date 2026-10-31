package com.aether.ms_calculator.calculator.dto.input;

import java.math.BigDecimal;
import java.util.Set;

public record CostReductionForecastInputDTO(
    BigDecimal monthlyCost,
    Integer reductionPercentage,
    Integer periodMonths,
    Set<String> unknownFields
) {
}
