package com.aether.ms_calculator.calculator.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CostReductionForecastCalculatorTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final CostReductionForecastValidator validator = new CostReductionForecastValidator();
  private final CostReductionForecastCalculator calculator = new CostReductionForecastCalculator();

  @Test
  void calculate_halfUpMonthlySavingsBeforeTotals() throws Exception {
    CostReductionForecastParsed parsed =
        validator.validate(
            MAPPER.readTree(
                "{\"monthlyCost\":123.45,\"reductionPercentage\":15,\"periodMonths\":3}"));
    CostReductionForecastResult result = calculator.calculate(parsed);

    assertEquals(new BigDecimal("18.52"), result.monthlySavings());
    assertEquals(new BigDecimal("104.93"), result.newMonthlyCost());
    assertEquals(new BigDecimal("55.56"), result.totalSavings());
    assertEquals(3, result.projection().size());
    assertEquals(new BigDecimal("55.56"), result.projection().get(2).accumulatedSavings());
  }

  @Test
  void calculate_trailingZeroMonthlyCost_computedMoneyHasScaleTwo() {
    ObjectNode request = MAPPER.createObjectNode();
    request.set("monthlyCost", MAPPER.getNodeFactory().numberNode(new BigDecimal("100.100")));
    request.put("reductionPercentage", 10);
    request.put("periodMonths", 1);

    CostReductionForecastResult result = calculator.calculate(validator.validate(request));

    assertEquals(0, new BigDecimal("10.01").compareTo(result.monthlySavings()));
    assertEquals(2, result.monthlySavings().scale());
    assertEquals(0, new BigDecimal("90.09").compareTo(result.newMonthlyCost()));
    assertEquals(2, result.newMonthlyCost().scale());
    assertEquals(0, new BigDecimal("10.01").compareTo(result.totalSavings()));
    assertEquals(2, result.totalSavings().scale());
    assertEquals(2, result.projection().get(0).accumulatedSavings().scale());
  }
}
