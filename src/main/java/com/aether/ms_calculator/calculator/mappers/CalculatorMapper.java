package com.aether.ms_calculator.calculator.mappers;

import com.aether.ms_calculator.calculator.domain.PaybackResult;
import com.aether.ms_calculator.calculator.domain.RoiResult;
import com.aether.ms_calculator.calculator.dto.input.CalculateInvestmentInputDTO;
import com.aether.ms_calculator.calculator.dto.output.CalculatePaybackOutputDTO;
import com.aether.ms_calculator.calculator.dto.output.CalculateRoiOutputDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class CalculatorMapper {

  private static final ObjectMapper JACKSON2 = new ObjectMapper();
  private static final JsonMapper JACKSON3 = JsonMapper.builder().build();

  private CalculatorMapper() {}

  public static ObjectNode toRequestNode(CalculateInvestmentInputDTO input) {
    ObjectNode node = JACKSON2.createObjectNode();
    setIfPresent(node, "capex", input.getCapex());
    setIfPresent(node, "wacc_monthly", input.getWaccMonthly());
    setIfPresent(node, "cash_flows", input.getCashFlows());
    setIfPresent(node, "monthly_cash_flow", input.getMonthlyCashFlow());
    input.getUnknownFields().forEach((name, value) -> setIfPresent(node, name, value));
    return node;
  }

  public static CalculateRoiOutputDTO toRoiOutput(RoiResult result) {
    return new CalculateRoiOutputDTO(
        toJackson3(result.capexEcho()),
        toJackson3(result.waccMonthlyEcho()),
        result.months(),
        result.vpTotal(),
        result.vpl(),
        result.roiPercent(),
        result.viable());
  }

  public static CalculatePaybackOutputDTO toPaybackOutput(PaybackResult result) {
    return new CalculatePaybackOutputDTO(
        toJackson3(result.capexEcho()),
        toJackson3(result.waccMonthlyEcho()),
        result.months(),
        result.vpTotal(),
        result.paybackMonths(),
        result.viable());
  }

  private static void setIfPresent(ObjectNode node, String field, JsonNode value) {
    com.fasterxml.jackson.databind.JsonNode converted = toJackson2(value);
    if (converted != null) {
      node.set(field, converted);
    }
  }

  private static com.fasterxml.jackson.databind.JsonNode toJackson2(JsonNode value) {
    if (value == null || value.isMissingNode()) {
      return null;
    }
    if (value.isNumber()) {
      double number = value.doubleValue();
      if (!Double.isFinite(number)) {
        return JACKSON2.getNodeFactory().numberNode(number);
      }
    }
    try {
      return JACKSON2.readTree(value.toString());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to copy JSON node into the domain tree.", exception);
    }
  }

  private static JsonNode toJackson3(com.fasterxml.jackson.databind.JsonNode value) {
    if (value == null) {
      return null;
    }
    return JACKSON3.readTree(value.toString());
  }
}
