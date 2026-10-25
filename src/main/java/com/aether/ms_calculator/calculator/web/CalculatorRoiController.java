package com.aether.ms_calculator.calculator.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/calculator")
@RequiredArgsConstructor
public class CalculatorRoiController {

  private final CalculatorService calculatorService;

  @PostMapping("/roi")
  public ResponseEntity<ObjectNode> calculateRoi(@RequestBody JsonNode body) {
    return ResponseEntity.ok(calculatorService.calculateRoi(body));
  }
}
