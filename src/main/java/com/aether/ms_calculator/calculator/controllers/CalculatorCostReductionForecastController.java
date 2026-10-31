package com.aether.ms_calculator.calculator.controllers;

import com.aether.ms_calculator.calculator.dto.output.CostReductionForecastOutputDTO;
import com.aether.ms_calculator.calculator.dto.request.CostReductionForecastRequestDTO;
import com.aether.ms_calculator.calculator.mappers.CalculatorMapper;
import com.aether.ms_calculator.calculator.services.CostReductionForecastService;
import com.aether.ms_calculator.shared.docs.CalculatorControllerDocs;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "Calculator",
    description = "Discounted ROI, payback, gas catalog, and cost-reduction forecasting.")
@RestController
@RequiredArgsConstructor
public class CalculatorCostReductionForecastController
    implements CalculatorControllerDocs.CostReductionForecast {

  private final CostReductionForecastService costReductionForecastService;

  @Override
  @PostMapping("/cost-reduction/forecast")
  public ResponseEntity<CostReductionForecastOutputDTO> forecastCostReduction(
      @RequestBody CostReductionForecastRequestDTO body) {
    return ResponseEntity.ok(
        costReductionForecastService.forecast(
            CalculatorMapper.convertCostReductionForecastRequestToInput(body)));
  }
}
