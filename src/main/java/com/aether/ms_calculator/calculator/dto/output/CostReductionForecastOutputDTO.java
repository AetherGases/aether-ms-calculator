package com.aether.ms_calculator.calculator.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record CostReductionForecastOutputDTO(
    String currency,
    @JsonProperty("monthlyCost") BigDecimal monthlyCost,
    @JsonProperty("reductionPercentage") Integer reductionPercentage,
    @JsonProperty("periodMonths") Integer periodMonths,
    BigDecimal monthlySavings,
    BigDecimal newMonthlyCost,
    BigDecimal totalSavings,
    List<CostReductionProjectionOutputDTO> projection) {}
