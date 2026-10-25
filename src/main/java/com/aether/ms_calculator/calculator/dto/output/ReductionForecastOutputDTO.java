package com.aether.ms_calculator.calculator.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ReductionForecastOutputDTO(
    @JsonProperty("future_emission") FutureEmissionOutputDTO futureEmission,
    @JsonProperty("reduction_co2e") BigDecimal reductionCo2e) {}
