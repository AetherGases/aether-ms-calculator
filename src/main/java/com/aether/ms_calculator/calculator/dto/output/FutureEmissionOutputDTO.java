package com.aether.ms_calculator.calculator.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record FutureEmissionOutputDTO(
    int gas,
    @JsonProperty("volume_co2e") BigDecimal volumeCo2e,
    int scope) {}
