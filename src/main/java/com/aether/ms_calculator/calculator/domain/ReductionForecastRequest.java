package com.aether.ms_calculator.calculator.domain;

import java.math.BigDecimal;
import java.util.Set;

public record ReductionForecastRequest(
    Integer years, CurrentEmission currentEmission, Integer greenGas, Set<String> unknownFields) {

  public record CurrentEmission(
      Integer gas, BigDecimal volumeCo2e, Integer scope, Set<String> unknownFields) {}
}
