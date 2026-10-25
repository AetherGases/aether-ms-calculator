package com.aether.ms_calculator.calculator.domain;

import java.math.BigDecimal;

public record ReductionForecastResult(
    int greenGasId, int scope, BigDecimal futureVolumeCo2e, BigDecimal reductionCo2e) {}
