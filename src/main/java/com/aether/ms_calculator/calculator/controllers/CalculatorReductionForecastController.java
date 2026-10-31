package com.aether.ms_calculator.calculator.controllers;

import com.aether.ms_calculator.calculator.dto.output.ReductionForecastOutputDTO;
import com.aether.ms_calculator.calculator.dto.request.ReductionForecastRequestDTO;
import com.aether.ms_calculator.calculator.mappers.CalculatorMapper;
import com.aether.ms_calculator.calculator.services.ReductionForecastService;
import com.aether.ms_calculator.shared.docs.CalculatorControllerDocs;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Calculator", description = "Discounted ROI, payback, and gas catalog for reduction forecasting.")
@RestController
@RequiredArgsConstructor
public class CalculatorReductionForecastController implements CalculatorControllerDocs.ReductionForecast {

  private final ReductionForecastService reductionForecastService;

  @Override
  @PostMapping("/reduction/forecast")
  public ResponseEntity<ReductionForecastOutputDTO> forecastReduction(
      @RequestBody ReductionForecastRequestDTO body) {
    return ResponseEntity.ok(
        reductionForecastService.forecast(
            CalculatorMapper.convertReductionForecastRequestToInput(body)));
  }
}
