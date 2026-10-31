package com.aether.ms_calculator.calculator.dto.input;

import java.math.BigDecimal;
import java.util.Set;

public record ReductionForecastInputDTO(
    Integer years,
    CurrentEmissionInputDTO currentEmission,
    Integer greenGas,
    Set<String> unknownFields
) {

  public record CurrentEmissionInputDTO(
      Integer gas,
      BigDecimal volumeCo2e,
      Integer scope,
      Set<String> unknownFields
  ) {
  }
}
