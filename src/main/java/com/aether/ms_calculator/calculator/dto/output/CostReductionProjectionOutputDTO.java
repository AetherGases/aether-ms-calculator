package com.aether.ms_calculator.calculator.dto.output;

import java.math.BigDecimal;

public record CostReductionProjectionOutputDTO(int month, BigDecimal accumulatedSavings) {}
