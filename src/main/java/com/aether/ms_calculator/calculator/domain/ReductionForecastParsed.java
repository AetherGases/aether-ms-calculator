package com.aether.ms_calculator.calculator.domain;

public record ReductionForecastParsed(
    int years, int currentGasId, double volumeCo2e, int scope, int greenGasId) {}
