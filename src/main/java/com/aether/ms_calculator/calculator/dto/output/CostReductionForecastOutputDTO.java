package com.aether.ms_calculator.calculator.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record CostReductionForecastOutputDTO(
    String currency,
    @JsonProperty("monthlyCost") JsonNode monthlyCost,
    @JsonProperty("reductionPercentage") JsonNode reductionPercentage,
    @JsonProperty("periodMonths") JsonNode periodMonths,
    BigDecimal monthlySavings,
    BigDecimal newMonthlyCost,
    BigDecimal totalSavings,
    List<CostReductionProjectionOutputDTO> projection) {}
