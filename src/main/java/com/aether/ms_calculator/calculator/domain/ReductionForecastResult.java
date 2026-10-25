package com.aether.ms_calculator.calculator.domain;

public record ReductionForecastResult(
    int greenGasId, int scope, double futureVolumeCo2e, double reductionCo2e) {}
