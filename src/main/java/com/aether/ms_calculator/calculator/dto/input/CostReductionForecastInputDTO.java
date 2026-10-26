package com.aether.ms_calculator.calculator.dto.input;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;

public class CostReductionForecastInputDTO {

  @JsonProperty("monthlyCost")
  private JsonNode monthlyCost;

  @JsonProperty("reductionPercentage")
  private JsonNode reductionPercentage;

  @JsonProperty("periodMonths")
  private JsonNode periodMonths;

  private final Map<String, JsonNode> unknownFields = new LinkedHashMap<>();

  @JsonAnySetter
  public void addUnknownField(String name, JsonNode value) {
    unknownFields.put(name, value);
  }

  public JsonNode getMonthlyCost() {
    return monthlyCost;
  }

  public JsonNode getReductionPercentage() {
    return reductionPercentage;
  }

  public JsonNode getPeriodMonths() {
    return periodMonths;
  }

  public Map<String, JsonNode> getUnknownFields() {
    return Collections.unmodifiableMap(unknownFields);
  }
}
