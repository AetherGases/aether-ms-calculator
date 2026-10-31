package com.aether.ms_calculator.calculator.controllers;

import com.aether.ms_calculator.calculator.dto.input.CalculateInvestmentInputDTO;
import com.aether.ms_calculator.calculator.dto.output.CalculateRoiOutputDTO;
import com.aether.ms_calculator.calculator.services.CalculatorService;
import com.aether.ms_calculator.shared.docs.CalculatorControllerDocs;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Calculator", description = "Discounted ROI and payback for a 60-month investment.")
@RestController
@RequiredArgsConstructor
public class CalculatorRoiController implements CalculatorControllerDocs.Roi {

  private final CalculatorService calculatorService;

  @Override
  @PostMapping("/roi")
  public ResponseEntity<CalculateRoiOutputDTO> calculateRoi(
      @RequestBody CalculateInvestmentInputDTO body) {
    return ResponseEntity.ok(calculatorService.calculateRoi(body));
  }
}
