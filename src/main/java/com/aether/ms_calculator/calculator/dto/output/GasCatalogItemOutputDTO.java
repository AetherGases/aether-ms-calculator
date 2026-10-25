package com.aether.ms_calculator.calculator.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record GasCatalogItemOutputDTO(
    int id,
    String name,
    String formula,
    @JsonProperty("is_green") boolean isGreen,
    BigDecimal gwp) {}
