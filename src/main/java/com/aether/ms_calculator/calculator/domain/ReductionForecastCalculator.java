package com.aether.ms_calculator.calculator.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
public class ReductionForecastCalculator {

  public ReductionForecastResult calculate(
      ReductionForecastParsed parsed, CatalogGas currentGas, CatalogGas greenGas) {
    BigDecimal years = BigDecimal.valueOf(parsed.years());
    BigDecimal reductionCo2e = parsed.volumeCo2e().multiply(years);
    BigDecimal futureVolume =
        parsed
            .volumeCo2e()
            .multiply(greenGas.gwp())
            .divide(currentGas.gwp(), MathContext.DECIMAL64)
            .multiply(years);
    return new ReductionForecastResult(
        greenGas.id(), parsed.scope(), futureVolume, reductionCo2e);
  }
}
