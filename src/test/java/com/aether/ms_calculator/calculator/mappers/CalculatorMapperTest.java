package com.aether.ms_calculator.calculator.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aether.ms_calculator.calculator.domain.RoiResult;
import com.aether.ms_calculator.calculator.dto.input.CalculateInvestmentInputDTO;
import com.aether.ms_calculator.calculator.dto.output.CalculateRoiOutputDTO;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CalculatorMapperTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Test
  void toRequestNode_keepsKnownFieldsAndUnknowns() {
    CalculateInvestmentInputDTO input =
        MAPPER.readValue(
            "{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125],\"discount_rate\":1,\"tax\":2}",
            CalculateInvestmentInputDTO.class);

    ObjectNode node = CalculatorMapper.toRequestNode(input);

    assertEquals(150, node.get("capex").intValue());
    assertEquals(0.25, node.get("wacc_monthly").doubleValue());
    assertTrue(node.has("cash_flows"));
    assertFalse(node.has("monthly_cash_flow"));
    assertEquals(1, node.get("discount_rate").intValue());
    assertEquals(2, node.get("tax").intValue());
  }

  @Test
  void toRequestNode_omitsMissingFields() {
    CalculateInvestmentInputDTO input = MAPPER.readValue("{}", CalculateInvestmentInputDTO.class);

    ObjectNode node = CalculatorMapper.toRequestNode(input);

    assertFalse(node.has("capex"));
    assertFalse(node.has("wacc_monthly"));
    assertFalse(node.has("cash_flows"));
    assertFalse(node.has("monthly_cash_flow"));
  }

  @Test
  void toRequestNode_keepsNonFiniteCapexAsNumber() {
    CalculateInvestmentInputDTO input =
        MAPPER.readValue(
            "{\"capex\":1e309,\"wacc_monthly\":0.25,\"cash_flows\":[125]}",
            CalculateInvestmentInputDTO.class);

    ObjectNode node = CalculatorMapper.toRequestNode(input);

    assertTrue(node.get("capex").isNumber());
    assertEquals(Double.POSITIVE_INFINITY, node.get("capex").doubleValue());
  }

  @Test
  void toRoiOutput_echoesCapexNode() {
    IntNode capex = IntNode.valueOf(150);
    RoiResult result = new RoiResult(capex, IntNode.valueOf(0), 60, 200.0, 50.0, 33.33, true);

    CalculateRoiOutputDTO output = CalculatorMapper.toRoiOutput(result);

    assertEquals("150", output.capex().toString());
    assertEquals(200.0, output.vpTotal());
    assertEquals(33.33, output.roiPercent());
  }
}
