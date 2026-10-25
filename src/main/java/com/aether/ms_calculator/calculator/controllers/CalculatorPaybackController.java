package com.aether.ms_calculator.calculator.controllers;

import com.aether.ms_calculator.calculator.dto.input.CalculateInvestmentInputDTO;
import com.aether.ms_calculator.calculator.dto.output.CalculatePaybackOutputDTO;
import com.aether.ms_calculator.calculator.services.CalculatorService;
import com.aether.ms_calculator.shared.docs.CalculatorControllerDocs;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Calculator", description = "Discounted ROI and payback for a 60-month investment.")
@RestController
@RequestMapping("/calculator")
@RequiredArgsConstructor
public class CalculatorPaybackController implements CalculatorControllerDocs.Payback {

  private final CalculatorService calculatorService;

  @Override
  @PostMapping("/payback")
  public ResponseEntity<CalculatePaybackOutputDTO> calculatePayback(
      @RequestBody CalculateInvestmentInputDTO body) {
    return ResponseEntity.ok(calculatorService.calculatePayback(body));
  }
}
