package com.aether.ms_calculator.calculator.dto.input;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;

public class CalculateInvestmentInputDTO {

  @JsonProperty("capex")
  private JsonNode capex;

  @JsonProperty("wacc_monthly")
  private JsonNode waccMonthly;

  @JsonProperty("cash_flows")
  private JsonNode cashFlows;

  @JsonProperty("monthly_cash_flow")
  private JsonNode monthlyCashFlow;

  private final Map<String, JsonNode> unknownFields = new LinkedHashMap<>();

  @JsonAnySetter
  public void addUnknownField(String name, JsonNode value) {
    unknownFields.put(name, value);
  }

  public JsonNode getCapex() {
    return capex;
  }

  public JsonNode getWaccMonthly() {
    return waccMonthly;
  }

  public JsonNode getCashFlows() {
    return cashFlows;
  }

  public JsonNode getMonthlyCashFlow() {
    return monthlyCashFlow;
  }

  public Map<String, JsonNode> getUnknownFields() {
    return Collections.unmodifiableMap(unknownFields);
  }
}
