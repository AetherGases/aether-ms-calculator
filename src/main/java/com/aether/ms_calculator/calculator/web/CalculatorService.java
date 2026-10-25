package com.aether.ms_calculator.calculator.web;

import com.aether.ms_calculator.calculator.domain.InvestmentCalculator;
import com.aether.ms_calculator.calculator.domain.InvestmentParsed;
import com.aether.ms_calculator.calculator.domain.InvestmentResultJson;
import com.aether.ms_calculator.calculator.domain.InvestmentValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalculatorService {

  private final InvestmentValidator investmentValidator;
  private final InvestmentCalculator investmentCalculator;

  public ObjectNode calculateRoi(JsonNode body) {
    InvestmentParsed parsed = investmentValidator.parse(body);
    return InvestmentResultJson.fromRoi(investmentCalculator.calculateRoi(parsed));
  }

  public ObjectNode calculatePayback(JsonNode body) {
    InvestmentParsed parsed = investmentValidator.parse(body);
    return InvestmentResultJson.fromPayback(investmentCalculator.calculatePayback(parsed));
  }
}
