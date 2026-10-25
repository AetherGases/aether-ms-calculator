package com.aether.ms_calculator.calculator.dto.input;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;

public class ReductionForecastInputDTO {

  @JsonProperty("years")
  private JsonNode years;

  @JsonProperty("current_emission")
  private JsonNode currentEmission;

  @JsonProperty("green_gas")
  private JsonNode greenGas;

  private final Map<String, JsonNode> unknownFields = new LinkedHashMap<>();

  @JsonAnySetter
  public void addUnknownField(String name, JsonNode value) {
    unknownFields.put(name, value);
  }

  public JsonNode getYears() {
    return years;
  }

  public JsonNode getCurrentEmission() {
    return currentEmission;
  }

  public JsonNode getGreenGas() {
    return greenGas;
  }

  public Map<String, JsonNode> getUnknownFields() {
    return Collections.unmodifiableMap(unknownFields);
  }
}
