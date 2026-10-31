package com.aether.ms_calculator.calculator.services;

import com.aether.ms_calculator.calculator.domain.CostReductionForecastCalculator;
import com.aether.ms_calculator.calculator.domain.CostReductionForecastParsed;
import com.aether.ms_calculator.calculator.domain.CostReductionForecastResult;
import com.aether.ms_calculator.calculator.domain.CostReductionForecastValidator;
import com.aether.ms_calculator.calculator.dto.input.CostReductionForecastInputDTO;
import com.aether.ms_calculator.calculator.dto.output.CostReductionForecastOutputDTO;
import com.aether.ms_calculator.calculator.mappers.CalculatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CostReductionForecastService {

  private final CostReductionForecastValidator costReductionForecastValidator;
  private final CostReductionForecastCalculator costReductionForecastCalculator;

  public CostReductionForecastOutputDTO forecast(CostReductionForecastInputDTO input) {
    CostReductionForecastParsed parsed =
        costReductionForecastValidator.validate(
            CalculatorMapper.toCostReductionRequest(input));
    CostReductionForecastResult result = costReductionForecastCalculator.calculate(parsed);
    return CalculatorMapper.toCostReductionOutput(result);
  }
}
