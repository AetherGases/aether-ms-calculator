package com.aether.ms_calculator.calculator.domain;

import java.math.BigDecimal;

public record ReductionForecastParsed(
    int years, int currentGasId, BigDecimal volumeCo2e, int scope, int greenGasId) {}
