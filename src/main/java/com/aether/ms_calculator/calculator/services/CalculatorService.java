package com.aether.ms_calculator.calculator.services;

import com.aether.ms_calculator.calculator.domain.InvestmentCalculator;
import com.aether.ms_calculator.calculator.domain.InvestmentParsed;
import com.aether.ms_calculator.calculator.domain.InvestmentValidator;
import com.aether.ms_calculator.calculator.dto.input.CalculateInvestmentInputDTO;
import com.aether.ms_calculator.calculator.dto.output.CalculatePaybackOutputDTO;
import com.aether.ms_calculator.calculator.dto.output.CalculateRoiOutputDTO;
import com.aether.ms_calculator.calculator.mappers.CalculatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalculatorService {

  private final InvestmentValidator investmentValidator;
  private final InvestmentCalculator investmentCalculator;

  public CalculateRoiOutputDTO calculateRoi(CalculateInvestmentInputDTO input) {
    InvestmentParsed parsed = investmentValidator.parse(CalculatorMapper.toRequestNode(input));
    return CalculatorMapper.toRoiOutput(investmentCalculator.calculateRoi(parsed));
  }

  public CalculatePaybackOutputDTO calculatePayback(CalculateInvestmentInputDTO input) {
    InvestmentParsed parsed = investmentValidator.parse(CalculatorMapper.toRequestNode(input));
    return CalculatorMapper.toPaybackOutput(investmentCalculator.calculatePayback(parsed));
  }
}
