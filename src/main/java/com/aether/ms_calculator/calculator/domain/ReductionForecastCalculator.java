package com.aether.ms_calculator.calculator.domain;

import com.aether.ms_calculator.shared.persistence.postgres.entities.GasEntity;
import org.springframework.stereotype.Component;

@Component
public class ReductionForecastCalculator {

  public ReductionForecastResult calculate(
      ReductionForecastParsed parsed, GasEntity currentGas, GasEntity greenGas) {
    double gwpCurrent = currentGas.getGwp().doubleValue();
    double gwpGreen = greenGas.getGwp().doubleValue();
    double reductionCo2e = parsed.volumeCo2e() * parsed.years();
    double futureVolume =
        parsed.volumeCo2e() * (gwpGreen / gwpCurrent) * parsed.years();
    return new ReductionForecastResult(
        greenGas.getId(), parsed.scope(), futureVolume, reductionCo2e);
  }
}
